package top.guyi.iot.ipojo.compile.lib.configuration.parse;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javassist.ClassPool;
import org.apache.commons.io.IOUtils;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.SyncContext;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.deployment.DeployRequest;
import org.eclipse.aether.deployment.DeployResult;
import org.eclipse.aether.deployment.DeploymentException;
import org.eclipse.aether.installation.InstallRequest;
import org.eclipse.aether.installation.InstallResult;
import org.eclipse.aether.installation.InstallationException;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.*;
import top.guyi.iot.ipojo.application.utils.StringUtils;
import top.guyi.iot.ipojo.compile.lib.compile.exception.CompileInfoCheckException;
import top.guyi.iot.ipojo.compile.lib.configuration.Compile;
import top.guyi.iot.ipojo.compile.lib.configuration.entry.Dependency;
import top.guyi.iot.ipojo.compile.lib.enums.CompileType;
import top.guyi.iot.ipojo.compile.lib.configuration.entry.Project;
import top.guyi.iot.ipojo.compile.lib.maven.MavenHelper;

import java.io.*;
import java.lang.reflect.Method;
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

    private void addDependencies(Map<String,Object> configuration,Project project,ClassPool pool){
        try{
            if (configuration.containsKey("project")){
                Project new_project = this.gson.fromJson(
                        this.gson.toJson(configuration.get("project")),
                        Project.class
                );
                if (new_project.getRepositories().isEmpty()){
                    new_project.setRepositories(project.getRepositories());
                }
                if (StringUtils.isEmpty(new_project.getLocalRepository())){
                    new_project.setLocalRepository(project.getLocalRepository());
                }
                Set<Dependency> dependencies = new HashSet<>();
                new_project.getDependencies()
                        .forEach(dependency ->
                                dependencies.addAll(MavenHelper.getDependencies(new_project,dependency)));

                project.getDependencies().addAll(dependencies);
            }

            URLClassLoader classLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
            Method add = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            add.setAccessible(true);
            // 添加编译期依赖
            add.invoke(classLoader,new URL(String.format("file:///%s",project.getWork())));
            for (Dependency dependency : project.getDependencies()) {
                pool.appendClassPath(dependency.get(project));
                add.invoke(classLoader,dependency.getURL(project));
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public Compile create(Project project, ClassPool pool) throws IOException, CompileInfoCheckException {
        File file = new File(project.getWork() + "/compile.info");
        if (!file.exists()){
            file = new File(project.getBaseDir() + "/compile.info");
        }

        Compile compile = this.create(file,project,pool);
        compile.setType(Optional.ofNullable(compile.getType()).orElse(CompileType.COMPONENT));
        compile.getProject().extend(project);

        return compile;
    }

    public Compile create(File file, Project project, ClassPool pool) throws IOException, CompileInfoCheckException {
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

        if (configuration.containsKey("profile")){
            if (configuration.get("profile") instanceof List){
                extendProfile(configuration,new HashSet<>((List<String>)configuration.get("profile")),project);
            }else{
                extendProfile(configuration,Collections.singleton(configuration.get("profile").toString()),project);
            }
        }

        this.addDependencies(configuration,project,pool);

        getExtendConfiguration(configuration,getAllConfiguration(configuration,project));

        Compile compile = CompileFormat.format(configuration);
        project.getDependencies().addAll(compile.getDependencies());

        if (StringUtils.isEmpty(compile.getPackageName())){
            throw new CompileInfoCheckException("packageName");
        }
        return compile;
    }

    private void extendProfile(Map<String,Object> configuration, Set<String> profileNames,Project project) throws IOException {
        Set<String> names = new HashSet<>(profileNames);
        names.add("default");
        ClassLoader loader = this.getClassLoader(project);
        for (String name : names) {
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
