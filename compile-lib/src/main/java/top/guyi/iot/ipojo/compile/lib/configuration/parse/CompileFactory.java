package top.guyi.iot.ipojo.compile.lib.configuration.parse;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javassist.ClassPool;
import javassist.NotFoundException;
import org.apache.commons.io.IOUtils;
import top.guyi.iot.ipojo.application.utils.StringUtils;
import top.guyi.iot.ipojo.compile.lib.compile.exception.CompileInfoCheckException;
import top.guyi.iot.ipojo.compile.lib.configuration.Compile;
import top.guyi.iot.ipojo.compile.lib.configuration.entry.Dependency;
import top.guyi.iot.ipojo.compile.lib.enums.CompileType;
import top.guyi.iot.ipojo.compile.lib.configuration.entry.Project;
import top.guyi.iot.ipojo.compile.lib.maven.MavenHelper;
import top.guyi.iot.ipojo.compile.lib.utils.AttachUtils;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class CompileFactory {

    private final Gson gson = new Gson();

    /**
     * 继承排除字段
     */
    private final List<String> excludeFields = Arrays.asList(
            "name",
            "type",
            "extends",
            "symbolicName"
    );

    /**
     * 获取添加了依赖的类加载器
     * @param project 项目实体
     * @return 类加载器
     */
    private URLClassLoader getClassLoader(Project project){
        Set<URL> urls = project.getDependencies().stream()
                .map(dependency -> dependency.getURL(project).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        try {
            urls.add(new URL(String.format("file:///%s",project.getWork())));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return new URLClassLoader(urls.toArray(new URL[0]));
    }

    /**
     * 添加依赖
     * @param configuration 编译配置
     * @param project 项目实体
     * @param pool javassist
     */
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
                if (new_project.getServers().isEmpty()){
                    new_project.setServers(project.getServers());
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
                dependency.get(project).ifPresent(path -> {
                    try {
                        pool.appendClassPath(path);
                    } catch (NotFoundException e) {
                        e.printStackTrace();
                    }
                });
                dependency.getURL(project).ifPresent(url -> {
                    try {
                        add.invoke(classLoader,url);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                });
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 创建编译配置实体
     * @param project 项目实体
     * @param pool javassist
     * @return 编译配置实体
     * @throws IOException
     * @throws CompileInfoCheckException
     */
    public Compile create(Project project, ClassPool pool) throws IOException, CompileInfoCheckException {
        File file = new File(project.getWork() + "/ipojo.compile");
        if (!file.exists()){
            file = new File(project.getBaseDir() + "/ipojo.compile");
        }

        Compile compile = this.create(file,project,pool);
        compile.setType(Optional.ofNullable(compile.getType()).orElse(CompileType.COMPONENT));
        compile.getProject().extend(project);

        return compile;
    }

    /**
     * 创建编译配置实体
     * @param file 编译配置文件
     * @param project 项目实体
     * @param pool javassist
     * @return 编译配置实体
     * @throws IOException
     * @throws CompileInfoCheckException
     */
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

        if (configuration.containsKey("attach")){
            if (configuration.get("attach") instanceof List){
                extendProfile(configuration,new HashSet<>((List<String>)configuration.get("attach")),project);
            }else{
                extendProfile(configuration,Collections.singleton(configuration.get("attach").toString()),project);
            }
        }

        this.addDependencies(configuration,project,pool);

        extendConfiguration(configuration,getAllConfiguration(project));

        Compile compile = CompileFormat.format(configuration);
        project.getDependencies().addAll(compile.getDependencies());

        if (StringUtils.isEmpty(compile.getPackageName())){
            throw new CompileInfoCheckException("packageName");
        }
        return compile;
    }

    /**
     * 继承Attach配置
     * @param configuration 原始配置
     * @param names attach文件名称或路径列表
     * @param project 项目实体
     * @throws IOException
     */
    private void extendProfile(Map<String,Object> configuration, Set<String> names,Project project) throws IOException {
        ClassLoader loader = this.getClassLoader(project);
        for (String name : names) {
            AttachUtils.getProfileInputStream(loader,project.getBaseDir(),name).ifPresent(inputStream -> {
                try {
                    Map<String,Object> profileConfiguration = this.gson.fromJson(
                            IOUtils.toString(inputStream, StandardCharsets.UTF_8)
                            ,new TypeToken<Map<String,Object>>(){}.getType());
                    profileConfiguration.forEach((key,value) -> {
                        if (!this.excludeFields.contains(key)){
                            configuration.put(key,ExtendFieldFactory.extend(value,configuration.get(key)));
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }


    /**
     * 获取继承的编译配置
     * @param project 项目实体
     * @return 继承的编译配置
     * @throws IOException
     */
    private Map<String,Map<String,Object>> getAllConfiguration(Project project) throws IOException {
        URLClassLoader loader = this.getClassLoader(project);
        Map<String,Map<String,Object>> configurations = new HashMap<>();
        Enumeration<URL> enumeration = loader.getResources("ipojo.compile");
        while (enumeration.hasMoreElements()){
            Map<String,Object> config = this.getConfiguration(
                    IOUtils.toString(enumeration.nextElement().openStream(),StandardCharsets.UTF_8)
            );
            configurations.put(this.getName(config),config);
        }
        return configurations;
    }

    /**
     * 获取项目自身的编译配置
     * @param json 内容json
     * @return 项目自身的编译配置
     */
    private Map<String,Object> getConfiguration(String json){
        return gson.fromJson(json,new TypeToken<Map<String,Object>>(){}.getType());
    }

    /**
     * 获取项目名称
     * @param configuration 编译配置
     * @return 项目名称
     */
    private String getName(Map<String,Object> configuration){
        return configuration.getOrDefault("name","ipojo-bundle").toString();
    }

    /**
     * 继承编译配置
     * @param configuration 项目自身的编译配置
     * @param configurations 继承的编译配置
     */
    private void extendConfiguration(Map<String,Object> configuration, Map<String,Map<String,Object>> configurations){
        configurations.values()
                .stream()
                .map(Map::entrySet)
                .flatMap(Collection::stream)
                .forEach(e -> {
                            if (!this.excludeFields.contains(e.getKey())){
                                configuration.put(e.getKey(),ExtendFieldFactory.extend(e.getValue(),configuration.get(e.getKey())));
                            }
                        });
    }

}
