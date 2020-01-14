package top.guyi.iot.ipojo.compile.lib.configuration.parse;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.io.IOUtils;
import top.guyi.iot.ipojo.application.utils.StringUtils;
import top.guyi.iot.ipojo.compile.lib.compile.exception.CompileInfoCheckException;
import top.guyi.iot.ipojo.compile.lib.configuration.Compile;
import top.guyi.iot.ipojo.compile.lib.enums.CompileType;
import top.guyi.iot.ipojo.compile.lib.configuration.entry.Project;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class CompileFactory {

    private Gson gson = new Gson();

    private List<String> excludeFields = Arrays.asList(
            "name",
            "type",
            "extends",
            "symbolicName"
    );

    private URLClassLoader getClassLoader(Project project){
        Set<URL> urls = project.getDependencies().stream()
                .map(dependency -> {
                    try {
                        return dependency.getURL(project);
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        try {
            urls.add(new URL(String.format("file:///%s",project.getWork())));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return new URLClassLoader(urls.toArray(new URL[0]));
    }

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
        if (StringUtils.isEmpty(configuration.getOrDefault("package","").toString())){
            throw new CompileInfoCheckException("package");
        }
        if (StringUtils.isEmpty(configuration.getOrDefault("name","").toString())){
            throw new CompileInfoCheckException("name");
        }

        CompileType.getByValue(configuration.getOrDefault("type","component").toString())
                .orElseThrow(() -> new RuntimeException("错误的编译类型"));

        getExtendConfiguration(configuration,getAllConfiguration(configuration,project));

        if (configuration.containsKey("profile")){
            if (configuration.get("profile") instanceof List){
                extendProfile(configuration,new HashSet<>((List<String>)configuration.get("profile")),project);
            }else{
                extendProfile(configuration,Collections.singleton(configuration.get("profile").toString()),project);
            }
        }

        Compile compile = CompileFormat.format(configuration);
        project.getDependencies().addAll(compile.getDependencies());

        if (StringUtils.isEmpty(compile.getPackageName())){
            throw new CompileInfoCheckException("packageName");
        }
        return compile;
    }

    private void extendProfile(Map<String,Object> configuration, Set<String> profileNames,Project project) throws IOException {
        ClassLoader loader = this.getClassLoader(project);
        for (String name : profileNames) {
            InputStream inputStream = loader.getResourceAsStream(String.format("%s.profile",name));
            if (inputStream != null){
                Map<String,Object> profileConfiguration = this.gson.fromJson(
                        IOUtils.toString(inputStream,StandardCharsets.UTF_8)
                        ,new TypeToken<Map<String,Object>>(){}.getType());
                profileConfiguration.forEach((key,value) -> {
                    if (!this.excludeFields.contains(key)){
                        configuration.put(key,ExtendFieldFactory.extend(value,configuration.get(key)));
                    }
                });
            }
        }
    }

    private Map<String,Map<String,Object>> getAllConfiguration(Map<String,Object> configuration,Project project) throws IOException {
        URLClassLoader loader = this.getClassLoader(project);
        Map<String,Map<String,Object>> configurations = new HashMap<>();
        Enumeration<URL> enumeration = loader.getResources("compile.info");
        while (enumeration.hasMoreElements()){
            Map<String,Object> config = this.getConfiguration(
                    IOUtils.toString(enumeration.nextElement().openStream(),StandardCharsets.UTF_8)
            );
            configurations.put(this.getName(config),config);
        }
        return configurations;
    }

    private Map<String,Object> getConfiguration(String json){
        return gson.fromJson(json,new TypeToken<Map<String,Object>>(){}.getType());
    }

    private String getName(Map<String,Object> configuration){
        return configuration.getOrDefault("name","ipojo-bundle").toString();
    }

    private Set<String> getExtendNames(Map<String,Object> configuration){
        Set<String> extendsName = Optional.ofNullable(configuration.get("extends"))
                .map(value -> {
                    if (value instanceof List){
                        return new HashSet<>((List<String>)value);
                    }else if ((value instanceof Boolean) && !((Boolean)value)){
                        return new HashSet<String>();
                    }else{
                        return new HashSet<>(Arrays.asList(value.toString().split(",")));
                    }
                })
                .orElseGet(() -> {
                    if (!"ipojo".equals(configuration.get("name"))){
                        return new HashSet<>(Collections.singleton("ipojo"));
                    }else{
                        return new HashSet<>();
                    }
                });
        configuration.put("extends",extendsName);
        return extendsName;
    }

    private Map<String,Object> getExtendConfiguration(Map<String,Object> configuration,Map<String,Map<String,Object>> configurations){
        configurations.values()
                .stream()
                .map(Map::entrySet)
                .flatMap(Collection::stream)
                .forEach(e -> {
                            if (!this.excludeFields.contains(e.getKey())){
                                configuration.put(e.getKey(),ExtendFieldFactory.extend(e.getValue(),configuration.get(e.getKey())));
                            }
                        });
        return configuration;
    }

}
