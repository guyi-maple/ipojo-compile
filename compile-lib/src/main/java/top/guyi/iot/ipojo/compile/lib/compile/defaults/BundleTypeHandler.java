package top.guyi.iot.ipojo.compile.lib.compile.defaults;

import top.guyi.iot.ipojo.application.ApplicationContext;
import top.guyi.iot.ipojo.application.bean.ComponentInfo;
import top.guyi.iot.ipojo.application.bean.interfaces.ApplicationStartEvent;
import top.guyi.iot.ipojo.application.bean.interfaces.ApplicationStartSuccessEvent;
import top.guyi.iot.ipojo.application.bean.interfaces.ApplicationStopEvent;
import top.guyi.iot.ipojo.application.bean.interfaces.InitializingBean;
import top.guyi.iot.ipojo.application.osgi.DefaultApplicationActivator;
import top.guyi.iot.ipojo.application.osgi.env.EnvMap;
import top.guyi.iot.ipojo.compile.lib.compile.entry.*;
import top.guyi.iot.ipojo.compile.lib.compile.CompileTypeHandler;
import javassist.*;
import org.osgi.framework.BundleContext;
import top.guyi.iot.ipojo.compile.lib.configuration.Compile;
import top.guyi.iot.ipojo.compile.lib.enums.CompileType;

import java.util.HashMap;
import java.util.Map;
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

        // 注册组件
        components
                .stream()
                .filter(CompileClass::isComponent)
                .forEach(component -> registerMethodBody.append(this.getRegisterMethod(component)));
        registerMethodBody.append("}\n");

        registerMethod.setBody(registerMethodBody.toString());
        activator.addMethod(registerMethod);

        //实现afterPropertiesSet方法
//        StringBuffer afterPropertiesSetMethodBody = new StringBuffer("{");
//        CtClass initializingBeanClass = pool.get(InitializingBean.class.getName());
//        components
//                .stream()
//                .filter(CompileClass::isComponent)
//                .filter(component -> {
//                    try {
//                        return component.getClasses().subtypeOf(initializingBeanClass);
//                    } catch (NotFoundException e) {
//                        e.printStackTrace();
//                        return false;
//                    }
//                })
//                .forEach(component -> afterPropertiesSetMethodBody.append(
//                        String.format(
//                                "((%s)$1.get(%s.class,true)).afterPropertiesSet();\n",
//                                InitializingBean.class.getName(),
//                                component.getClasses().getName()
//                        )
//                ));
//        afterPropertiesSetMethodBody.append("}");
//        CtMethod afterPropertiesSetMethod = new CtMethod(CtClass.voidType,"onAfterPropertiesSet",new CtClass[]{
//                pool.get(ApplicationContext.class.getName()),
//                pool.get(BundleContext.class.getName())
//        },activator);
//        afterPropertiesSetMethod.setBody(afterPropertiesSetMethodBody.toString());
//        activator.addMethod(afterPropertiesSetMethod);

        //实现onStart方法
        StringBuffer onStartMethodBody = new StringBuffer("{");
        CtClass onStartClass = pool.get(ApplicationStartEvent.class.getName());
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
                                ApplicationStartEvent.class.getName(),
                                component.getClasses().getName()
                        )
                ));
        onStartMethodBody.append("}");
        CtMethod onStartMethod = new CtMethod(CtClass.voidType,"onStart",new CtClass[]{
                pool.get(ApplicationContext.class.getName()),
                pool.get(BundleContext.class.getName())
        },activator);
        onStartMethod.setBody(onStartMethodBody.toString());
        activator.addMethod(onStartMethod);

        //实现onStartSuccess方法
        StringBuffer onStartSuccessMethodBody = new StringBuffer("{");
        CtClass onStartSuccessClass = pool.get(ApplicationStartSuccessEvent.class.getName());
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
                                ApplicationStartSuccessEvent.class.getName(),
                                component.getClasses().getName()
                        )
                ));
        onStartSuccessMethodBody.append("}");
        CtMethod onStartSuccessMethod = new CtMethod(CtClass.voidType,"onStartSuccess",new CtClass[]{
                pool.get(ApplicationContext.class.getName()),
                pool.get(BundleContext.class.getName())
        },activator);
        onStartSuccessMethod.setBody(onStartSuccessMethodBody.toString());
        activator.addMethod(onStartSuccessMethod);

        // 实现onStop方法
        StringBuffer onStopMethodBody = new StringBuffer("{");
        CtClass onStopCLasses = pool.get(ApplicationStopEvent.class.getName());
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
                                ApplicationStopEvent.class.getName(),
                                component.getClasses().getName()
                        )
                ));
        onStopMethodBody.append("}");
        CtMethod onStopMethod = new CtMethod(CtClass.voidType,"onStop",new CtClass[]{
                pool.get(ApplicationContext.class.getName()),
                pool.get(BundleContext.class.getName())
        },activator);
        onStopMethod.setBody(onStopMethodBody.toString());
        activator.addMethod(onStopMethod);


        // 实现getName方法
        CtMethod getNameMethod = new CtMethod(pool.get(String.class.getName()), "getName", new CtClass[0], activator);
        getNameMethod.setBody(String.format("{return \"%s\";}", compile.getName()));
        activator.addMethod(getNameMethod);

        // 实现getEnv方法
        CtMethod getEnvMethod = new CtMethod(pool.get(EnvMap.class.getName()),"getEnv",new CtClass[0],activator);
        StringBuffer sb = new StringBuffer();
        sb.append("{\n");
        sb.append(String.format("%s env = new %s();\n",EnvMap.class.getName(), EnvMap.class.getName()));
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
                ComponentInfo.class.getName(),
                component.getName(),
                component.getOrder(),
                component.isProxy()
        );
    }

}
