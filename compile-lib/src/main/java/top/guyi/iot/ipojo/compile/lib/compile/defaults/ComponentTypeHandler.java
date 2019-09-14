package top.guyi.iot.ipojo.compile.lib.compile.defaults;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import top.guyi.iot.ipojo.compile.lib.classes.ClassCompiler;
import top.guyi.iot.ipojo.compile.lib.compile.entry.*;
import top.guyi.iot.ipojo.compile.lib.compile.CompileTypeHandler;
import javassist.ClassPool;
import org.apache.commons.io.IOUtils;
import top.guyi.iot.ipojo.compile.lib.configuration.CompileInfo;
import top.guyi.iot.ipojo.compile.lib.compile.entry.ComponentInfo;
import top.guyi.iot.ipojo.compile.lib.enums.CompileType;
import top.guyi.iot.ipojo.compile.lib.project.configuration.ProjectInfo;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 组件编译处理器
 */
public class ComponentTypeHandler implements CompileTypeHandler {

    @Override
    public CompileType forType() {
        return CompileType.COMPONENT;
    }

    private Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private ClassCompiler classCompiler = new ClassCompiler();

    @Override
    public Set<CompileClass> handle(ClassPool pool, String path, CompileInfo compileInfo, ProjectInfo projectInfo, Set<CompileClass> components) throws IOException {
        ComponentInfo componentInfo = new ComponentInfo();

        componentInfo.setComponents(
                this.classCompiler.compile(pool,path)
                        .stream()
                        .map(component -> new ComponentEntry(component.getClasses().getName()))
                        .collect(Collectors.toSet())
        );

        PrintWriter writer = new PrintWriter(path + "component.info");
        IOUtils.write(this.gson.toJson(componentInfo),writer);
        writer.flush();
        writer.close();

        return components;
    }

}
