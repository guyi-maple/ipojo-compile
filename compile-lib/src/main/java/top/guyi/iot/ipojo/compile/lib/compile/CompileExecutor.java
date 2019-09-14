package top.guyi.iot.ipojo.compile.lib.compile;

import com.google.gson.Gson;
import top.guyi.iot.ipojo.application.utils.StringUtils;
import top.guyi.iot.ipojo.compile.lib.classes.ClassCompiler;
import top.guyi.iot.ipojo.compile.lib.compile.defaults.BundleTypeHandler;
import top.guyi.iot.ipojo.compile.lib.compile.defaults.ComponentTypeHandler;
import top.guyi.iot.ipojo.compile.lib.compile.entry.CompileClass;
import top.guyi.iot.ipojo.compile.lib.configuration.CompileInfo;
import top.guyi.iot.ipojo.compile.lib.project.entry.Dependency;
import top.guyi.iot.ipojo.compile.lib.project.configuration.ProjectInfo;
import top.guyi.iot.ipojo.compile.lib.enums.CompileType;
import top.guyi.iot.ipojo.compile.lib.expand.CompileExpand;
import top.guyi.iot.ipojo.compile.lib.compile.exception.CompileInfoFileNotFoundException;
import javassist.ClassPool;
import org.apache.commons.io.IOUtils;
import top.guyi.iot.ipojo.compile.lib.expand.ManifestExpand;
import top.guyi.iot.ipojo.compile.lib.manifest.ManifestWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 编译执行者
 */
public class CompileExecutor {

    private Gson gson = new Gson();
    private ClassCompiler compiler = new ClassCompiler();
    private ManifestWriter manifestWriter = new ManifestWriter();

    private Map<CompileType, CompileTypeHandler> typeHandlers;
    private List<CompileExpand> compileExpands = new LinkedList<>();
    private List<ManifestExpand> manifestExpands = new LinkedList<>();

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

    public CompileInfo getCompileInfo(String path,ProjectInfo projectInfo) throws IOException {
        File file = Optional.of(new File(path + "/compile.info"))
                .filter(File::exists)
                .orElse(new File(projectInfo.getBaseDir() + "/compile.info"));
        if (file.exists()){
            return this.gson.fromJson(IOUtils.toString(new FileInputStream(file), StandardCharsets.UTF_8), CompileInfo.class);
        }
        return null;
    }

    /**
     * 执行编译
     * @param path 项目Class文件路径
     * @throws Exception
     */
    public CompileInfo execute(String path, ProjectInfo projectInfo) throws Exception {
        path = path.replace("\\","/");
        path = path.endsWith("/") ? path : path + "/";

        projectInfo = Optional.ofNullable(projectInfo).orElseGet(ProjectInfo::new);
        CompileInfo compileInfo = this.getCompileInfo(path,projectInfo);
        if (compileInfo != null){
            if (StringUtils.isEmpty(compileInfo.getOutput())){
                compileInfo.setOutput(path);
            }

            // 检查编译信息配置是否错误
            compileInfo.check();

            ClassPool pool = ClassPool.getDefault();
            pool.appendClassPath(path);

            URLClassLoader classLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
            Method add = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            add.setAccessible(true);
            // 添加编译期依赖
            add.invoke(classLoader,new URL(String.format("file:///%s",path)));
            for (Dependency dependency : projectInfo.getDependencies()) {
                pool.appendClassPath(dependency.getPath());
                add.invoke(classLoader,new URL(String.format("file:///%s",dependency.getPath())));
            }

            // 获取组件列表
            Set<CompileClass> components = this.compiler.compile(pool,path);

            // 执行拓展
            for (CompileExpand expand : this.compileExpands) {
                components = expand.execute(pool,path, compileInfo,components);
            }

            // 执行编译处理器
            CompileTypeHandler handler = this.typeHandlers.get(compileInfo.getType());
            if (handler != null) {
                handler.handle(pool,path, compileInfo,projectInfo, components);
            }

            // 写出类文件
            for (CompileClass component : components) {
                if (component.isWrite()){
                    component.getClasses().writeFile(compileInfo.getOutput());
                }
            }

            manifestWriter.write(pool,components,compileInfo,projectInfo,this.manifestExpands);
        }

        return compileInfo;
    }

}
