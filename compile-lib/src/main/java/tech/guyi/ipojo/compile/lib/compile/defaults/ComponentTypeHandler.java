package tech.guyi.ipojo.compile.lib.compile.defaults;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javassist.NotFoundException;
import tech.guyi.ipojo.compile.lib.compile.entry.CompileClass;
import tech.guyi.ipojo.compile.lib.compile.entry.ComponentEntry;
import tech.guyi.ipojo.compile.lib.configuration.Compile;
import tech.guyi.ipojo.compile.lib.enums.CompileType;
import tech.guyi.ipojo.compile.lib.compile.CompileTypeHandler;
import javassist.ClassPool;
import org.apache.commons.io.IOUtils;
import tech.guyi.ipojo.compile.lib.compile.entry.ComponentInfo;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 组件编译处理器
 */
public class ComponentTypeHandler implements CompileTypeHandler {

    private Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public boolean check(Compile compile) {
        return compile.getType() == CompileType.COMPONENT;
    }

    @Override
    public Set<CompileClass> handle(ClassPool pool, Compile compile, Set<CompileClass> components) throws IOException, NotFoundException {
        ComponentInfo componentInfo = new ComponentInfo();

        componentInfo.setConfigurations(compile.getConfigurationKeys());

        componentInfo.setComponents(
                components.stream()
                        .map(component -> new ComponentEntry("classes",component.getClasses().getName(),component.isProxy()))
                        .collect(Collectors.toSet())
        );

        componentInfo.setUseComponents(
                compile.filterUseComponents(components)
                        .stream()
                        .map(component -> new ComponentEntry("classes",component.getClasses().getName(),component.isProxy()))
                        .collect(Collectors.toSet())
        );

        componentInfo.setName(compile.getName());

        File target = new File(compile.getProject().getOutput() + "/component.info");
        if (!target.getParentFile().exists()){
            target.getParentFile().mkdirs();
        }

        PrintWriter writer = new PrintWriter(target);
        IOUtils.write(this.gson.toJson(componentInfo),writer);
        writer.flush();
        writer.close();

        return components;
    }

}
