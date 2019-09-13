package top.guyi.iot.ipojo.compile.lib.compile;

import com.google.gson.Gson;
import top.guyi.iot.ipojo.application.utils.StringUtils;
import top.guyi.iot.ipojo.compile.lib.classes.ClassCompiler;
import top.guyi.iot.ipojo.compile.lib.compile.defaults.BundleTypeHandler;
import top.guyi.iot.ipojo.compile.lib.compile.defaults.ComponentTypeHandler;
import top.guyi.iot.ipojo.compile.lib.compile.entry.CompileInfo;
import top.guyi.iot.ipojo.compile.lib.compile.entry.Dependency;
import top.guyi.iot.ipojo.compile.lib.compile.entry.ProjectInfo;
import top.guyi.iot.ipojo.compile.lib.enums.CompileType;
import top.guyi.iot.ipojo.compile.lib.expand.CompileExpand;
import top.guyi.iot.ipojo.compile.lib.compile.exception.CompileInfoFileNotFoundException;
import javassist.ClassPool;
import javassist.CtClass;
import org.apache.commons.io.IOUtils;
import top.guyi.iot.ipojo.compile.lib.expand.ManifestExpand;
import top.guyi.iot.ipojo.compile.lib.manifest.ManifestWriter;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * 编译执行者
 */
public class CompileExecutor {

    private Gson gson = new Gson();
    private ClassCompiler compiler = new ClassCompiler();
    private ManifestWriter manifestWriter = new ManifestWriter();

    private CompileInfo compileInfo;
    private Map<CompileType, CompileTypeHandler> typeHandlers;
    private List<CompileInfoSetter> compileInfoSetters = new LinkedList<>();
    private List<CompileExpand> compileExpands = new LinkedList<>();
    private List<ManifestExpand> manifestExpands = new LinkedList<>();

    /**
     * 添加编译信息设置器
     * @param setter 编译信息设置器
     */
    public void compileInfoSetter(CompileInfoSetter setter){
        this.compileInfoSetters.add(setter);
    }

    /**
     * 添加编译拓展
     * @param expand 编译拓展
     */
    public void compileExpand(CompileExpand expand){
        this.compileExpands.add(expand);
        this.compileExpands.sort(Comparator.comparingInt(CompileExpand::order));
    }

    /**
     * 添加MANIFEST文件拓展
     * @param expand 编译拓展
     */
    public void manifestExpand(ManifestExpand expand){
        this.manifestExpands.add(expand);
        this.manifestExpands.sort(Comparator.comparingInt(ManifestExpand::order));
    }


    public CompileExecutor() {
        this.typeHandlers = new HashMap<>();

        ComponentTypeHandler componentTypeHandler = new ComponentTypeHandler();
        this.typeHandlers.put(componentTypeHandler.forType(), componentTypeHandler);

        BundleTypeHandler bundleTypeHandler = new BundleTypeHandler();
        this.typeHandlers.put(bundleTypeHandler.forType(),bundleTypeHandler);
    }

    /**
     * 执行编译
     * @param path 项目Class文件路径
     * @throws Exception
     */
    public void execute(String path, ProjectInfo projectInfo) throws Exception {
        path = path.endsWith("/") ? path : path + "/";

        Optional.ofNullable(projectInfo)
                .ifPresent(info -> info.setFinalName(this.getFinalName(projectInfo)));

        File file = new File(path + "/compile.info");
        if (file.exists()) {
            compileInfo = this.gson.fromJson(IOUtils.toString(new FileInputStream(file)), CompileInfo.class);

            if (StringUtils.isEmpty(compileInfo.getOutput())){
                compileInfo.setOutput(path);
            }

            // 执行编译信息设置器
            this.compileInfoSetters.forEach(setter ->
                compileInfo = setter.set(compileInfo)
            );

            // 检查编译信息配置是否错误
            compileInfo.check();

            ClassPool pool = ClassPool.getDefault();
            pool.appendClassPath(path);

            URLClassLoader classLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
            Method add = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            add.setAccessible(true);
            // 添加编译期依赖
            for (Dependency dependency : Optional.ofNullable(this.compileInfo.getDependencies())
                    .orElse(new LinkedHashSet<>())) {
                pool.appendClassPath(dependency.getPath());
                add.invoke(classLoader,new URL(String.format("file:///%s",dependency.getPath())));
            }

            // 获取组件列表
            Set<CtClass> components = this.compiler.compile(pool,path);

            // 执行拓展
            for (CompileExpand expand : this.compileExpands) {
                components = expand.execute(pool,path, compileInfo,components);
            }

            // 执行编译处理器
            CompileTypeHandler handler = this.typeHandlers.get(compileInfo.getType());
            if (handler != null) {
                handler.handle(pool,path, compileInfo, components);
            }

            // 写出类文件
            for (CtClass component : components) {
                component.writeFile(compileInfo.getOutput());
            }

            manifestWriter.write(compileInfo,projectInfo,this.manifestExpands);

            if (compileInfo.isDelete()) {
                //移除编译信息文件
                Files.delete(Paths.get(file.getAbsolutePath()));
            }

        } else {
            throw new CompileInfoFileNotFoundException();
        }
    }

    public String getFinalName(ProjectInfo info) {

        return null;
    }

}
