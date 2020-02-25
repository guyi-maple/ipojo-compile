package top.guyi.iot.ipojo.compile.lib;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import top.guyi.iot.ipojo.compile.lib.classes.ClassCompiler;
import top.guyi.iot.ipojo.compile.lib.compile.CompileTypeHandler;
import top.guyi.iot.ipojo.compile.lib.compile.CompileTypeHandlerFactory;
import top.guyi.iot.ipojo.compile.lib.compile.entry.CompileClass;
import top.guyi.iot.ipojo.compile.lib.configuration.Compile;
import top.guyi.iot.ipojo.compile.lib.configuration.entry.Dependency;
import top.guyi.iot.ipojo.compile.lib.configuration.entry.Project;
import top.guyi.iot.ipojo.compile.lib.configuration.parse.CompileFactory;
import top.guyi.iot.ipojo.compile.lib.enums.JdkVersion;
import top.guyi.iot.ipojo.compile.lib.expand.compile.CompileExpand;
import javassist.ClassPool;
import top.guyi.iot.ipojo.compile.lib.expand.compile.CompileExpandFactory;
import top.guyi.iot.ipojo.compile.lib.expand.manifest.ManifestExpandFactory;

import java.io.*;
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
        ClassPool pool = ClassPool.getDefault();
        Compile compile = compileFactory.create(project,pool);

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
            if (compile.isFormatJdkVersion()){
                for (CompileClass component : components) {
                    if (component.isWrite()){
                        this.formatJavaVersion(component.getClasses().getURL(),compile.getJdk());
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
                    new File(compile.getProject().getOutput() + "/compile.info"),
                    gson.toJson(compile),
                    StandardCharsets.UTF_8
            );
        }
        return Optional.ofNullable(compile);
    }

    private void formatJavaVersion(URL url, JdkVersion version) throws IOException {
        byte[] arr = IOUtils.toByteArray(url.openStream());
        arr[7] = (byte)version.getTarget();
        OutputStream out = new FileOutputStream(url.getFile());
        out.write(arr);
        out.flush();
        out.close();
    }


}
