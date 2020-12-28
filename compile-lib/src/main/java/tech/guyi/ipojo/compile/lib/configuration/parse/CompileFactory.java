package tech.guyi.ipojo.compile.lib.configuration.parse;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javassist.ClassPool;
import javassist.NotFoundException;
import org.apache.commons.io.IOUtils;
import tech.guyi.ipojo.compile.lib.configuration.entry.Project;
import tech.guyi.ipojo.compile.lib.enums.CompileType;
import tech.guyi.ipojo.compile.lib.maven.MavenHelper;
import tech.guyi.ipojo.compile.lib.utils.FileUtils;
import tech.guyi.ipojo.compile.lib.utils.StringUtils;
import tech.guyi.ipojo.compile.lib.compile.exception.CompileInfoCheckException;
import tech.guyi.ipojo.compile.lib.configuration.Compile;
import tech.guyi.ipojo.compile.lib.configuration.entry.Dependency;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class CompileFactory {

    private final Gson gson = new Gson();
    private final ClassPool pool;

    /**
     * 继承排除字段
     */
    private final List<String> excludeFields = Arrays.asList(
            "name",
            "type",
            "extends",
            "symbolicName"
    );

    public CompileFactory(ClassPool pool){
        this.pool = pool;
    }

    /**
     * 添加依赖
     * @param configuration 编译配置
     * @param project 项目实体
     */
    private void addDependencies(Map<String,Object> configuration, Project project){
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

            project.getDependencies()
                    .stream()
                    .map(d -> d.get(project).orElse(null))
                    .filter(Objects::nonNull)
                    .forEach(path -> {
                        try {
                            this.pool.appendClassPath(path);
                        } catch (NotFoundException e) {
                            e.printStackTrace();
                        }
                    });

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 创建编译配置实体
     * @param project 项目实体
     * @return 编译配置实体
     * @throws IOException
     * @throws CompileInfoCheckException
     */
    public Compile create(Project project) throws IOException, CompileInfoCheckException {
        File file = new File(project.getWork() + "/ipojo.compile");
        if (!file.exists()){
            file = new File(project.getBaseDir() + "/ipojo.compile");
        }

        if (!file.exists()){
            return null;
        }

        Compile compile = this.create(file,project);
        compile.setType(Optional.ofNullable(compile.getType()).orElse(CompileType.COMPONENT));
        compile.getProject().extend(project);

        return compile;
    }

    /**
     * 创建编译配置实体
     * @param file 编译配置文件
     * @param project 项目实体
     * @return 编译配置实体
     * @throws IOException
     * @throws CompileInfoCheckException
     */
    public Compile create(File file, Project project) throws IOException, CompileInfoCheckException {
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

        this.addDependencies(configuration,project);

        extendConfiguration(configuration,getAllConfiguration(project));

        Compile compile = CompileFormat.format(configuration);

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
        for (String name : names) {
            FileUtils.getAttachContent(project,name).ifPresent(attach -> {
                Map<String,Object> profileConfiguration = this.gson.fromJson(attach,new TypeToken<Map<String,Object>>(){}.getType());
                profileConfiguration.forEach((key,value) -> {
                    if (!this.excludeFields.contains(key)){
                        configuration.put(key,ExtendFieldFactory.extend(value,configuration.get(key)));
                    }
                });
            });
        }
    }


    /**
     * 获取继承的编译配置
     * @param project 项目
     * @return 继承的编译配置
     */
    private Map<String,Map<String,Object>> getAllConfiguration(Project project) {
        Map<String,Map<String,Object>> configuration = new HashMap<>();
        FileUtils.getCompileFileContents(project)
                .stream()
                .map(this::getConfiguration)
                .forEach(config -> configuration.put(this.getName(config),config));
        return configuration;
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
