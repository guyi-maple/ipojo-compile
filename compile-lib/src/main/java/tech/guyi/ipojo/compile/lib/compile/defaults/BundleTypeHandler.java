package tech.guyi.ipojo.compile.lib.compile.defaults;

import tech.guyi.ipojo.compile.lib.compile.entry.CompileClass;
import tech.guyi.ipojo.compile.lib.configuration.Compile;
import tech.guyi.ipojo.compile.lib.cons.ClassNames;
import tech.guyi.ipojo.compile.lib.enums.CompileType;
import tech.guyi.ipojo.compile.lib.compile.CompileTypeHandler;
import javassist.*;

import java.util.Set;
import java.util.stream.Collectors;

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
                pool.get(ClassNames.ApplicationContext),
                pool.get(ClassNames.BundleContext)
        }, activator);
        // 继承拓展抽象类
        activator.setSuperclass(pool.get(ClassNames.AbstractApplicationActivator));

        // 注册组件
        Set<CompileClass> tmpComponents = compile.filterUseComponents(components)
                .stream()
                .filter(CompileClass::isComponent)
                .collect(Collectors.toSet());
        StringBuilder registerMethodBody = new StringBuilder("{\n");
        tmpComponents
                .stream()
                .filter(component -> component.isRegister(pool,tmpComponents))
                .forEach(component -> registerMethodBody.append(this.getRegisterMethod(component)));
        registerMethodBody.append("}\n");

        registerMethod.setBody(registerMethodBody.toString());
        activator.addMethod(registerMethod);

        //实现onStart方法
        StringBuffer onStartMethodBody = new StringBuffer("{");
        CtClass onStartClass = pool.get(ClassNames.ApplicationStartEvent);
        components
                .stream()
                .filter(CompileClass::isComponent)
                .filter(component -> {
                    try {
                        return component.getClasses().subtypeOf(onStartClass);
                    } catch (NotFoundException e) {
                        e.printStackTrace();
                        return false;
                    }
                })
                .forEach(component -> onStartMethodBody.append(
                        String.format(
                                "((%s)$1.get(%s.class,true)).onStart($1,$2);\n",
                                ClassNames.ApplicationStartEvent,
                                component.getClasses().getName()
                        )
                ));
        onStartMethodBody.append("}");
        CtMethod onStartMethod = new CtMethod(CtClass.voidType,"onStart",new CtClass[]{
                pool.get(ClassNames.ApplicationContext),
                pool.get(ClassNames.BundleContext)
        },activator);
        onStartMethod.setExceptionTypes(new CtClass[]{pool.get(Exception.class.getName())});
        onStartMethod.setBody(onStartMethodBody.toString());
        activator.addMethod(onStartMethod);

        //实现onStartSuccess方法
        StringBuffer onStartSuccessMethodBody = new StringBuffer("{");
        CtClass onStartSuccessClass = pool.get(ClassNames.ApplicationStartSuccessEvent);
        components
                .stream()
                .filter(CompileClass::isComponent)
                .filter(component -> {
                    try {
                        return component.getClasses().subtypeOf(onStartSuccessClass);
                    } catch (NotFoundException e) {
                        e.printStackTrace();
                        return false;
                    }
                })
                .forEach(component -> onStartSuccessMethodBody.append(
                        String.format(
                                "((%s)$1.get(%s.class,true)).onStartSuccess($1,$2);\n",
                                ClassNames.ApplicationStartSuccessEvent,
                                component.getClasses().getName()
                        )
                ));
        onStartSuccessMethodBody.append("}");
        CtMethod onStartSuccessMethod = new CtMethod(CtClass.voidType,"onStartSuccess",new CtClass[]{
                pool.get(ClassNames.ApplicationContext),
                pool.get(ClassNames.BundleContext)
        },activator);
        onStartSuccessMethod.setExceptionTypes(new CtClass[]{pool.get(Exception.class.getName())});
        onStartSuccessMethod.setBody(onStartSuccessMethodBody.toString());
        activator.addMethod(onStartSuccessMethod);

        // 实现onStop方法
        StringBuffer onStopMethodBody = new StringBuffer("{");
        CtClass onStopCLasses = pool.get(ClassNames.ApplicationStopEvent);
        components
                .stream()
                .filter(CompileClass::isComponent)
                .filter(component -> {
                    try {
                        return component.getClasses().subtypeOf(onStopCLasses);
                    } catch (NotFoundException e) {
                        e.printStackTrace();
                        return false;
                    }
                })
                .forEach(component -> onStopMethodBody.append(
                        String.format(
                                "((%s)$1.get(%s.class,true)).onStop($1,$2);\n",
                                ClassNames.ApplicationStopEvent,
                                component.getClasses().getName()
                        )
                ));
        onStopMethodBody.append("}");
        CtMethod onStopMethod = new CtMethod(CtClass.voidType,"onStop",new CtClass[]{
                pool.get(ClassNames.ApplicationContext),
                pool.get(ClassNames.BundleContext)
        },activator);
        onStopMethod.setBody(onStopMethodBody.toString());
        activator.addMethod(onStopMethod);


        // 实现getName方法
        CtMethod getNameMethod = new CtMethod(pool.get(String.class.getName()), "getName", new CtClass[0], activator);
        getNameMethod.setBody(String.format("{return \"%s\";}", compile.getName()));
        activator.addMethod(getNameMethod);

        // 实现getEnv方法
        CtMethod getEnvMethod = new CtMethod(pool.get(ClassNames.EnvMap),"getEnv",new CtClass[0],activator);
        StringBuffer sb = new StringBuffer();
        sb.append("{\n");
        sb.append(String.format("%s env = new %s();\n",ClassNames.EnvMap, ClassNames.EnvMap));
        compile.getEnv().forEach((key,value) ->
                sb.append(String.format("env.put(\"%s\",\"%s\");\n",key,value)));
        sb.append("return env; \n}");
        getEnvMethod.setBody(sb.toString());
        activator.addMethod(getEnvMethod);

        components.add(new CompileClass(activator));

        compile.setActivator(activator.getName());
    }

    private String getRegisterMethod(CompileClass component){
        return String.format(
                "$1.register().put(%s.class,new %s(\"%s\",%s,%s));\n",
                component.getClasses().getName(),
                ClassNames.ComponentInfo,
                component.getName(),
                component.getOrder(),
                component.isProxy()
        );
    }

}
