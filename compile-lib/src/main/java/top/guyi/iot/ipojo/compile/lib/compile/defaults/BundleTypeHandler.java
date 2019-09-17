package top.guyi.iot.ipojo.compile.lib.compile.defaults;

import top.guyi.iot.ipojo.application.ApplicationContext;
import top.guyi.iot.ipojo.application.bean.ComponentInfo;
import top.guyi.iot.ipojo.application.osgi.DefaultApplicationActivator;
import top.guyi.iot.ipojo.compile.lib.compile.entry.*;
import top.guyi.iot.ipojo.compile.lib.compile.CompileTypeHandler;
import javassist.*;
import org.osgi.framework.BundleContext;
import top.guyi.iot.ipojo.compile.lib.configuration.Compile;
import top.guyi.iot.ipojo.compile.lib.enums.CompileType;

import java.util.Set;

/**
 * Bundle编译处理器
 */
public class BundleTypeHandler implements CompileTypeHandler {

    @Override
    public boolean check(Compile compile) {
        return compile.getType() == CompileType.BUNDLE;
    }

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
        StringBuilder registerMethodBody = new StringBuilder("{\n");

//        // 注册支持库组件
//        for (Dependency dependency : compile.getProject().getDependencies()) {
//            JarFile jar = new JarFile(dependency.getPath());
//            ZipEntry entry = jar.getEntry("component.info");
//            if (entry != null) {
//                ComponentInfo componentInfo = this.gson.fromJson(
//                        IOUtils.toString(jar.getInputStream(entry), StandardCharsets.UTF_8), ComponentInfo.class);
//                Optional.ofNullable(componentInfo.getComponents())
//                        .ifPresent(componentEntries -> componentEntries.forEach(component -> {
//                            if ("classes".equals(component.getType())) {
//                                registerMethodBody.append(this.getRegisterMethod(component));
//                            }
//                        }));
//            }
//        }

        // 注册组件
        components
                .stream()
                .filter(CompileClass::isComponent)
                .forEach(component -> registerMethodBody.append(this.getRegisterMethod(component)));
        registerMethodBody.append("}\n");

        registerMethod.setBody(registerMethodBody.toString());
        activator.addMethod(registerMethod);

        // 实现getName方法
        CtMethod getNameMethod = new CtMethod(pool.get(String.class.getName()), "getName", new CtClass[0], activator);
        getNameMethod.setBody(String.format("{return \"%s\";}", compile.getName()));
        activator.addMethod(getNameMethod);

        components.add(new CompileClass(activator));

        compile.setActivator(activator.getName());
    }

    private String getRegisterMethod(CompileClass component){
        return String.format(
                "$1.register().put(%s.class,new %s(\"%s\",%s,%s));\n",
                component.getClasses().getName(),
                ComponentInfo.class.getName(),
                component.getName(),
                component.getOrder(),
                component.isProxy()
        );
    }

}
