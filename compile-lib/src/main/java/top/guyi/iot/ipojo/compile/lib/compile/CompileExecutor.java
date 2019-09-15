package top.guyi.iot.ipojo.compile.lib.compile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.FileUtils;
import top.guyi.iot.ipojo.compile.lib.classes.ClassCompiler;
import top.guyi.iot.ipojo.compile.lib.compile.defaults.BundleTypeHandler;
import top.guyi.iot.ipojo.compile.lib.compile.defaults.ComponentTypeHandler;
import top.guyi.iot.ipojo.compile.lib.compile.entry.CompileClass;
import top.guyi.iot.ipojo.compile.lib.configuration.Compile;
import top.guyi.iot.ipojo.compile.lib.configuration.entry.Dependency;
import top.guyi.iot.ipojo.compile.lib.configuration.entry.Project;
import top.guyi.iot.ipojo.compile.lib.configuration.parse.CompileFactory;
import top.guyi.iot.ipojo.compile.lib.enums.CompileType;
import top.guyi.iot.ipojo.compile.lib.expand.CompileExpand;
import javassist.ClassPool;
import top.guyi.iot.ipojo.compile.lib.expand.ManifestExpand;
import top.guyi.iot.ipojo.compile.lib.manifest.ManifestWriter;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 编译执行者
 */
public class CompileExecutor {

    private CompileFactory compileFactory = new CompileFactory();
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

    /**
     * 执行编译
     * @param project 项目信息
     * @throws Exception
     */
    public Optional<Compile> execute(Project project) throws Exception {
        Compile compile = compileFactory.create(project);

        if (compile != null){
            ClassPool pool = ClassPool.getDefault();
            pool.appendClassPath(compile.getProject().getWork());

            URLClassLoader classLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
            Method add = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            add.setAccessible(true);
            // 添加编译期依赖
            add.invoke(classLoader,new URL(String.format("file:///%s",compile.getProject().getWork())));
            for (Dependency dependency : project.getDependencies()) {
                pool.appendClassPath(dependency.getPath());
                add.invoke(classLoader,new URL(String.format("file:///%s",dependency.getPath())));
            }

            // 获取组件列表
            Set<CompileClass> components = this.compiler.compile(pool,compile);

            // 执行拓展
            for (CompileExpand expand : this.compileExpands) {
                components = expand.execute(pool,compile,components);
            }

            // 执行编译处理器
            CompileTypeHandler handler = this.typeHandlers.get(compile.getType());
            if (handler != null) {
                handler.handle(pool, compile, components);
            }

            // 写出类文件
            for (CompileClass component : components) {
                if (component.isWrite()){
                    component.getClasses().writeFile(compile.getProject().getOutput());
                }
            }

            manifestWriter.write(pool,components, compile,this.manifestExpands);

            Gson gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .excludeFieldsWithoutExposeAnnotation()
                    .create();
            FileUtils.write(
                    new File(compile.getProject().getOutput() + "/compile.info"),
                    gson.toJson(compile),
                    StandardCharsets.UTF_8
            );
        }
        return Optional.ofNullable(compile);
    }

}
