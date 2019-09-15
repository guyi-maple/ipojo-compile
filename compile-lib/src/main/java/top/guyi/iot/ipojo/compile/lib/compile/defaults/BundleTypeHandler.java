package top.guyi.iot.ipojo.compile.lib.compile.defaults;

import com.google.gson.Gson;
import top.guyi.iot.ipojo.application.ApplicationContext;
import top.guyi.iot.ipojo.application.osgi.DefaultApplicationActivator;
import top.guyi.iot.ipojo.compile.lib.compile.entry.*;
import top.guyi.iot.ipojo.compile.lib.compile.CompileTypeHandler;
import javassist.*;
import org.apache.commons.io.IOUtils;
import org.osgi.framework.BundleContext;
import top.guyi.iot.ipojo.compile.lib.configuration.Compile;
import top.guyi.iot.ipojo.compile.lib.compile.entry.ComponentInfo;
import top.guyi.iot.ipojo.compile.lib.enums.CompileType;
import top.guyi.iot.ipojo.compile.lib.configuration.entry.Dependency;

import java.util.Optional;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * Bundle编译处理器
 */
public class BundleTypeHandler implements CompileTypeHandler {

    @Override
    public CompileType forType() {
        return CompileType.BUNDLE;
    }

    private Gson gson = new Gson();

    @Override
    public Set<CompileClass> handle(ClassPool pool, Compile compile, Set<CompileClass> components) throws Exception {
        this.createActivator(pool,compile, components);
        return components;
    }

    /**
     * 创建OSGI-Activator
     * @param pool
     * @param compile
     * @param components
     * @throws Exception
     */
    private void createActivator(ClassPool pool,Compile compile,Set<CompileClass> components) throws Exception {
        // 创建类对象
        CtClass activator = pool.makeClass(String.format("%s.Activator", compile.getPackageName()));
        CtMethod registerMethod = new CtMethod(CtClass.voidType, "registerComponent", new CtClass[]{
                pool.get(ApplicationContext.class.getName()),
                pool.get(BundleContext.class.getName())
        }, activator);
        // 继承拓展抽象类
        activator.setSuperclass(pool.get(DefaultApplicationActivator.class.getName()));
        // 注册支持库组件
        StringBuilder registerMethodBody = new StringBuilder("{\n");
        for (Dependency dependency : compile.getProject().getDependencies()) {
            JarFile jar = new JarFile(dependency.getPath());
            ZipEntry entry = jar.getEntry("component.info");
            if (entry != null) {
                ComponentInfo componentInfo = this.gson.fromJson(IOUtils.toString(jar.getInputStream(entry)), ComponentInfo.class);
                Optional.ofNullable(componentInfo.getComponents())
                        .ifPresent(componentEntries -> componentEntries.forEach(component -> {
                            if ("classes".equals(component.getType())) {
                                registerMethodBody.append(String.format("$1.register(%s.class);\n", component.getClasses()));
                            }
                        }));
            }
        }
        // 注册组件
        components
                .stream()
                .filter(CompileClass::isComponent)
                .forEach(component -> registerMethodBody.append(String.format("$1.register(%s.class);\n", component.getClasses().getName())));
        registerMethodBody.append("}\n");

        registerMethod.setBody(registerMethodBody.toString());
        activator.addMethod(registerMethod);

        // 实现getName方法
        CtMethod getNameMethod = new CtMethod(pool.get(String.class.getName()), "getName", new CtClass[0], activator);
        getNameMethod.setBody(String.format("{return \"%s\";}", compile.getProject().getName()));
        activator.addMethod(getNameMethod);

        components.add(new CompileClass(activator));

        compile.setActivator(activator.getName());
    }

}
