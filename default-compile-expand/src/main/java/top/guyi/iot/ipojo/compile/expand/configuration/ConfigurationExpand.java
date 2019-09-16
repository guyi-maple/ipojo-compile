package top.guyi.iot.ipojo.compile.expand.configuration;

import javassist.*;
import top.guyi.iot.ipojo.application.ApplicationContext;
import top.guyi.iot.ipojo.application.osgi.configuration.ConfigurationRefreshInvoker;
import top.guyi.iot.ipojo.application.osgi.configuration.ConfigurationRefresher;
import top.guyi.iot.ipojo.application.osgi.configuration.annotation.ConfigurationKey;
import top.guyi.iot.ipojo.compile.expand.configuration.entry.ConfigurationField;
import top.guyi.iot.ipojo.compile.lib.compile.entry.CompileClass;
import top.guyi.iot.ipojo.compile.lib.configuration.Compile;
import top.guyi.iot.ipojo.compile.lib.expand.CompileExpand;
import top.guyi.iot.ipojo.compile.lib.utils.JavassistUtils;

import java.util.*;
import java.util.stream.Collectors;

public class ConfigurationExpand implements CompileExpand {

    @Override
    public Set<CompileClass> execute(ClassPool pool, Compile compile, Set<CompileClass> components) throws Exception {
        CtClass invokerSuper = pool.get(ConfigurationRefreshInvoker.class.getName());
        CtClass[] invokerConstructorParameterType = new CtClass[]{
                pool.get(Class.class.getName()),
                pool.get(String.class.getName())
        };
        CtClass[] invokerRefreshParameterType = new CtClass[]{
                pool.get(ApplicationContext.class.getName()),
                pool.get(Object.class.getName())
        };

        List<CompileClass> invokers = new LinkedList<>();
        StringBuffer methodBody = new StringBuffer("{");
        components.stream()
                .map(component -> Arrays.stream(component.getClasses().getDeclaredFields())
                        .map(field -> {
                            try {
                                ConfigurationKey key = (ConfigurationKey) field.getAnnotation(ConfigurationKey.class);
                                if (key != null){
                                    return new ConfigurationField(component.getClasses(),field,key);
                                }
                            } catch (ClassNotFoundException e) {
                                e.printStackTrace();
                            }
                            return null;
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()))
                .flatMap(Collection::stream)
                .forEach(field -> {
                    try {
                        String invokerName = String.format(
                                "%s.ConfigurationRefreshInvoker%s",
                                compile.getPackageName(),
                                UUID.randomUUID().toString().replaceAll("-",""));
                        CtClass invoker = pool.makeClass(invokerName);
                        invoker.setSuperclass(invokerSuper);

                        CtConstructor constructor = new CtConstructor(invokerConstructorParameterType,invoker);
                        constructor.setBody("{super($$);}");
                        invoker.addConstructor(constructor);

                        CtMethod method = new CtMethod(CtClass.voidType,"refresh",invokerRefreshParameterType,invoker);
                        String setMethodName = JavassistUtils.getSetMethod(field.getClasses(),field.getField()).getName();
                        method.setBody(String.format(
                                "{((%s)$1.get(%s.class,true)).%s((%s)$2);}",
                                field.getClasses().getName(),
                                field.getClasses().getName(),
                                setMethodName,
                                field.getField().getType().getName()
                        ));
                        invoker.addMethod(method);
                        invokers.add(new CompileClass(false,invoker));

                        methodBody.append(String.format(
                                "$0.registerInvoker(new %s(%s.class,\"%s\"));",
                                invokerName,
                                field.getField().getType().getName(),
                                field.getKey().key()
                        ));

                    } catch (CannotCompileException | NotFoundException e) {
                        e.printStackTrace();
                    }
                });
        methodBody.append("}");

        CtClass refresher = pool.makeClass(String.format("%s.DefaultAutoConfigurationRefresher",compile.getPackageName()));
        refresher.setSuperclass(pool.get(ConfigurationRefresher.class.getName()));
        CtMethod method = new CtMethod(CtClass.voidType,"registerInvokerAll",new CtClass[0],refresher);
        method.setBody(methodBody.toString());
        refresher.addMethod(method);

        components.add(new CompileClass(refresher,true,true,false));
        components.addAll(invokers);

        return components;
    }

}
