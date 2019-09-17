package top.guyi.iot.ipojo.compile.lib;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.FileUtils;
import top.guyi.iot.ipojo.compile.lib.classes.ClassCompiler;
import top.guyi.iot.ipojo.compile.lib.compile.CompileTypeHandler;
import top.guyi.iot.ipojo.compile.lib.compile.CompileTypeHandlerFactory;
import top.guyi.iot.ipojo.compile.lib.compile.entry.CompileClass;
import top.guyi.iot.ipojo.compile.lib.configuration.Compile;
import top.guyi.iot.ipojo.compile.lib.configuration.entry.Dependency;
import top.guyi.iot.ipojo.compile.lib.configuration.entry.Project;
import top.guyi.iot.ipojo.compile.lib.configuration.parse.CompileFactory;
import top.guyi.iot.ipojo.compile.lib.expand.compile.CompileExpand;
import javassist.ClassPool;
import top.guyi.iot.ipojo.compile.lib.expand.compile.CompileExpandFactory;
import top.guyi.iot.ipojo.compile.lib.expand.manifest.ManifestExpandFactory;

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

    private CompileTypeHandlerFactory compileTypeHandlerFactory;
    private CompileExpandFactory compileExpandFactory;
    private ManifestExpandFactory manifestExpandFactory;

    public CompileExecutor(){
        this.compileTypeHandlerFactory = new CompileTypeHandlerFactory();
        this.compileExpandFactory = new CompileExpandFactory();
        this.manifestExpandFactory = new ManifestExpandFactory();
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
            for (CompileExpand expand : this.compileExpandFactory.get(compile)) {
                components.addAll(expand.execute(pool,compile,components));
            }

            // 执行编译处理器
            for (CompileTypeHandler handler : this.compileTypeHandlerFactory.get(compile)) {
                components.addAll(handler.handle(pool, compile, components));
            }

            // 写出类文件
            for (CompileClass component : components) {
                if (component.isWrite()){
                    component.getClasses().writeFile(compile.getProject().getOutput());
                }
            }

            // 写出MANIFEST.INF文件
            this.manifestExpandFactory.write(pool,compile,components);

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
