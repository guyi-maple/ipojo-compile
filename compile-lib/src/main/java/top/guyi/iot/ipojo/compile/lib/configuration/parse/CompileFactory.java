package top.guyi.iot.ipojo.compile.lib.configuration.parse;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.io.IOUtils;
import top.guyi.iot.ipojo.application.utils.StringUtils;
import top.guyi.iot.ipojo.compile.lib.compile.exception.CompileInfoCheckException;
import top.guyi.iot.ipojo.compile.lib.configuration.Compile;
import top.guyi.iot.ipojo.compile.lib.enums.CompileType;
import top.guyi.iot.ipojo.compile.lib.configuration.entry.Project;
import top.guyi.iot.ipojo.compile.lib.configuration.entry.Dependency;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class CompileFactory {

    private Gson gson = new Gson();

    public Compile create(Project project) throws IOException, CompileInfoCheckException {
        File file = new File(project.getWork() + "/compile.info");
        if (!file.exists()){
            file = new File(project.getBaseDir() + "/compile.info");
        }

        Compile compile = this.create(file,project);
        compile.setType(Optional.ofNullable(compile.getType()).orElse(CompileType.COMPONENT));
        compile.getProject().extend(project);
        return compile;
    }

    public Compile create(File file,Project project) throws IOException, CompileInfoCheckException {
        if (!file.exists()){
            return null;
        }
        Map<String,Object> configuration = getConfiguration(IOUtils.toString(new FileInputStream(file), StandardCharsets.UTF_8));
        configuration = getExtendConfiguration(configuration,getAllConfiguration(project.getDependencies()));

        Compile compile = CompileFormat.format(configuration);
        if (StringUtils.isEmpty(compile.getPackageName())){
            throw new CompileInfoCheckException("packageName");
        }

        return compile;
    }

    private Map<String,Map<String,Object>> getAllConfiguration(Set<Dependency> dependencies) throws IOException {
        URL[] urls = dependencies.stream()
                .map(dependency -> {
                    try {
                        return new URL(String.format("file:///%s",dependency.getPath()));
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .toArray(URL[]::new);
        ClassLoader loader = new URLClassLoader(urls);
        Map<String,Map<String,Object>> configurations = new HashMap<>();
        Enumeration<URL> enumeration = loader.getResources("compile.info");
        while (enumeration.hasMoreElements()){
            Map<String,Object> configuration = this.getConfiguration(
                    IOUtils.toString(enumeration.nextElement().openStream(),StandardCharsets.UTF_8)
            );
            configurations.put(this.getName(configuration),configuration);
        }
        return configurations;
    }

    private Map<String,Object> getConfiguration(String json){
        return gson.fromJson(json,new TypeToken<Map<String,Object>>(){}.getType());
    }

    private String getName(Map<String,Object> configuration){
        return configuration.getOrDefault("name","ipojo-bundle").toString();
    }

    private Optional<String> getParentName(Map<String,Object> configuration){
        Optional<String> parent = Optional.ofNullable(configuration.get("parent"))
                .map(Object::toString);
        if (!parent.isPresent() && !"ipojo".equals(this.getName(configuration))){
            return Optional.of("ipojo");
        }
        return parent;
    }

    private Map<String,Object> getExtendConfiguration(Map<String,Object> configuration,Map<String,Map<String,Object>> configurations){
        this.getParentName(configuration)
                .flatMap(parent -> Optional.ofNullable(configurations.get(parent)))
                .ifPresent(parentConfiguration ->
                        getExtendConfiguration(parentConfiguration, configurations).forEach((key, value) ->
                                configuration.put(key, ExtendFieldFactory.extend(value, configuration.get(key)))
                        )
                );
        return configuration;
    }

}
