package top.guyi.ipojo.compile.lib;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.FileUtils;
import top.guyi.ipojo.compile.lib.classes.ClassCompiler;
import top.guyi.ipojo.compile.lib.compile.CompileTypeHandler;
import top.guyi.ipojo.compile.lib.compile.CompileTypeHandlerFactory;
import top.guyi.ipojo.compile.lib.compile.entry.CompileClass;
import top.guyi.ipojo.compile.lib.configuration.Compile;
import top.guyi.ipojo.compile.lib.configuration.entry.Project;
import top.guyi.ipojo.compile.lib.configuration.parse.CompileFactory;
import top.guyi.ipojo.compile.lib.enums.JdkVersion;
import top.guyi.ipojo.compile.lib.expand.compile.CompileExpand;
import javassist.ClassPool;
import top.guyi.ipojo.compile.lib.expand.compile.CompileExpandFactory;
import top.guyi.ipojo.compile.lib.expand.manifest.ManifestExpandFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 编译执行者
 */
public class CompileExecutor {

    private final ClassCompiler compiler = new ClassCompiler();

    private final CompileTypeHandlerFactory compileTypeHandlerFactory;
    private final CompileExpandFactory compileExpandFactory;
    private final ManifestExpandFactory manifestExpandFactory;

    public CompileExecutor(){
        this.compileTypeHandlerFactory = new CompileTypeHandlerFactory();
        this.compileExpandFactory = new CompileExpandFactory();
        this.manifestExpandFactory = new ManifestExpandFactory();
    }

    /**
     * 执行编译
     * @param project 项目实体
     * @throws Exception
     */
    public Optional<Compile> execute(Project project) throws Exception {
        ClassPool pool = new ClassPool();
        pool.appendSystemPath();
        pool.appendClassPath(project.getWork());
        CompileFactory compileFactory = new CompileFactory(pool);

        Compile compile = compileFactory.create(project);

        if (compile != null){
            pool.appendClassPath(compile.getProject().getWork());

            // 获取组件列表
            Set<CompileClass> components = this.compiler.compile(pool,compile);

            // 执行拓展
            for (CompileExpand expand : this.compileExpandFactory.get(compile)) {
                expand.execute(pool,compile,components);
            }

            // 执行编译处理器
            for (CompileTypeHandler handler : this.compileTypeHandlerFactory.get(compile)) {
                handler.handle(pool, compile, components);
            }

            // 写出类文件
            for (CompileClass component : components) {
                if (component.isWrite()){
                    component.getClasses().writeFile(compile.getProject().getOutput());
                }
            }

            // 更改Java版本
            if (compile.getJdk() != null && compile.getJdk() != JdkVersion.None){
                for (CompileClass component : components) {
                    if (component.isWrite()){
                        compile.formatJavaVersion(component.getClasses().getURL());
                    }
                }
            }

            // 写出MANIFEST.INF文件
            this.manifestExpandFactory.write(pool,compile,components);

            Gson gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .excludeFieldsWithoutExposeAnnotation()
                    .create();
            FileUtils.write(
                    new File(compile.getProject().getOutput() + "/ipojo.compile"),
                    gson.toJson(compile),
                    StandardCharsets.UTF_8
            );
        }

        return Optional.ofNullable(compile);
    }

}
