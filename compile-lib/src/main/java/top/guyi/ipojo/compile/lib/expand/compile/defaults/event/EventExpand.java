package top.guyi.ipojo.compile.lib.expand.compile.defaults.event;

import javassist.*;
import javassist.bytecode.annotation.ClassMemberValue;
import top.guyi.ipojo.compile.lib.compile.entry.CompileClass;
import top.guyi.ipojo.compile.lib.configuration.Compile;
import top.guyi.ipojo.compile.lib.cons.AnnotationNames;
import top.guyi.ipojo.compile.lib.cons.ClassNames;
import top.guyi.ipojo.compile.lib.enums.CompileType;
import top.guyi.ipojo.compile.lib.expand.compile.CompileExpand;
import top.guyi.ipojo.compile.lib.utils.AnnotationUtils;
import top.guyi.ipojo.compile.lib.utils.JavassistUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author guyi
 * 事件拓展
 */
public class EventExpand implements CompileExpand {

    @Override
    public boolean check(Compile compile) {
        return compile.getType() == CompileType.BUNDLE;
    }

    @Override
    public Set<CompileClass> execute(ClassPool pool, Compile compile, Set<CompileClass> components) throws Exception {
        CtClass register = pool.makeClass(String.format("%s.event.DefaultEventRegister", compile.getPackageName()));
        register.setSuperclass(pool.get(ClassNames.AbstractEventRegister));
        compile.addUseComponent(register);
        components.add(new CompileClass("DefaultEventRegister",register,true,true,false,998));


        CtClass converterClass = pool.get(ClassNames.EventConverter);
        Set<CtClass> converters = components.stream()
                .map(CompileClass::getClasses)
                .filter(classes -> JavassistUtils.equalsType(classes,converterClass))
                .collect(Collectors.toSet());

        this.registerConverters(register,converters);
        this.setEventListeners(register,pool,components);
        this.registerMethodEventListeners(register,pool,components, compile);
        this.createPublisher(pool, compile, components, converters);
        return components;
    }

    private void createPublisher(ClassPool pool, Compile compile, Set<CompileClass> components, Set<CtClass> converters) throws NotFoundException, CannotCompileException {
        CtClass publisher = pool.makeClass(String.format("%s.event.AutoEventPublisher", compile.getPackageName()));
        compile.addUseComponent(publisher);
        publisher.setSuperclass(pool.get(ClassNames.AbstractEventPublisher));
        CtMethod method = new CtMethod(CtClass.voidType,"setAllEventConverter",new CtClass[0],publisher);
        method.setModifiers(Modifier.PROTECTED);
        publisher.addMethod(method);

        StringBuilder methodBody = new StringBuilder("{");
        converters.stream()
                .map(converter -> String.format(
                        "$0.addConverter((%s)$0.applicationContext.get(%s.class,true));\n",
                        converter.getName(),
                        converter.getName()
                ))
                .forEach(methodBody::append);
        methodBody.append(String.format(
                "$0.addConverter(new %s());\n",
                ClassNames.DefaultEventConverter
        ));
        methodBody.append("}");
        method.setBody(methodBody.toString());

        components.add(new CompileClass(publisher));
    }

    private void registerConverters(CtClass register,Set<CtClass> converters) throws CannotCompileException {
        CtMethod method = new CtMethod(CtClass.voidType,"setAllConverter",new CtClass[0],register);
        method.setModifiers(Modifier.PROTECTED);
        register.addMethod(method);

        StringBuilder methodBody = new StringBuilder("{");
        converters.stream()
                .map(converter -> String.format(
                        "$0.setConverter((%s)$0.applicationContext.get(%s.class,true));\n",
                        converter.getName(),
                        converter.getName()))
                .forEach(methodBody::append);
        methodBody.append(String.format(
                "$0.setConverter(new %s());\n",
                ClassNames.DefaultEventConverter
        ));
        methodBody.append("}");

        method.setBody(methodBody.toString());
    }

    private void registerMethodEventListeners(CtClass register, ClassPool pool, Set<CompileClass> components, Compile compile) throws CannotCompileException {
        CtMethod method = new CtMethod(CtClass.voidType,"registerAllMethodListener",new CtClass[0],register);
        method.setModifiers(Modifier.PROTECTED);
        register.addMethod(method);

        StringBuffer methodBody = new StringBuffer("{");
        Set<CompileClass> invokers = components.stream()
                .map(component ->
                        Arrays.stream(component.getClasses().getDeclaredMethods())
                                .map(m -> {
                                    Set<CtClass> list = new HashSet<>();
                                    this.getMethodInvoker(pool,m,compile,component,methodBody)
                                            .ifPresent(list::add);
                                    this.getMethodNativeInvoker(pool,m,compile,component,methodBody)
                                            .ifPresent(list::add);
                                    return list;
                                })
                                .flatMap(Collection::stream)
                                .collect(Collectors.toSet()))
                .flatMap(Collection::stream)
                .map(invoker -> new CompileClass(false,invoker))
                .collect(Collectors.toSet());
        components.addAll(invokers);
        methodBody.append("}");

        method.setBody(methodBody.toString());
    }

    private void invokeMethod(CtClass invoker,CtClass bean,CtMethod method,CtClass eventClass,ClassPool pool,boolean isNative) throws NotFoundException, CannotCompileException {
        CtConstructor constructor;
        if (isNative){
            constructor = new CtConstructor(new CtClass[]{
                    pool.get(String.class.getName()),
                    pool.get(ClassNames.ApplicationContext)
            },invoker);
        }else{
            constructor = new CtConstructor(new CtClass[]{
                    pool.get(Class.class.getName()),
                    pool.get(ClassNames.ApplicationContext)
            },invoker);
        }
        constructor.setBody("{super($1,$2);}");
        invoker.addConstructor(constructor);

        CtMethod invokeMethod;
        if (isNative){
            invokeMethod = new CtMethod(CtClass.voidType,"invoke",new CtClass[]{
                    pool.get(ClassNames.ApplicationContext),
                    pool.get(ClassNames.NativeEvent)
            },invoker);
        }else{
            invokeMethod = new CtMethod(CtClass.voidType,"invoke",new CtClass[]{
                    pool.get(ClassNames.ApplicationContext),
                    pool.get(ClassNames.Event)
            },invoker);
        }
        invokeMethod.setExceptionTypes(new CtClass[]{pool.get(Exception.class.getName())});
        invokeMethod.setModifiers(Modifier.PROTECTED);
        invoker.addMethod(invokeMethod);

        StringBuilder argsBody = new StringBuilder();
        for (CtClass type : method.getParameterTypes()) {
            if (type.subtypeOf(eventClass)){
                argsBody.append(String.format("((%s)$2),",eventClass.getName()));
            }else{
                argsBody.append(String.format(
                        "((%s)$1.get(%s.class,true)),",
                        type.getName(),type.getName())
                );
            }
        }
        invokeMethod.setBody(String.format(
                "{((%s)$1.get(%s.class,true)).%s(%s);}",
                bean.getName(),
                bean.getName(),
                method.getName(),
                argsBody.substring(0,argsBody.length() - 1))
        );
    }

    private void setEventListeners(CtClass register,ClassPool pool,Set<CompileClass> components) throws NotFoundException, CannotCompileException {
        CtMethod setMethod = new CtMethod(CtClass.voidType,"registerAllListener",new CtClass[0],register);
        setMethod.setModifiers(Modifier.PROTECTED);
        register.addMethod(setMethod);

        CtClass listenerClass = pool.get(ClassNames.EventListener);
        StringBuffer setMethodBody = new StringBuffer("{");
        components.stream()
                .filter(component -> {
                    try {
                        return component.getClasses().subtypeOf(listenerClass);
                    } catch (NotFoundException e) {
                        e.printStackTrace();
                    }
                    return false;
                })
                .forEach(listener -> setMethodBody.append(String.format(
                        "$0.registerListener($0.bundleContext,(%s)$0.applicationContext.get(%s.class,true));\n",
                        listener.getClasses().getName(),
                        listener.getClasses().getName()
                )));
        setMethodBody.append("}");

        setMethod.setBody(setMethodBody.toString());
    }

    private Optional<CtClass> getMethodInvoker(ClassPool pool, CtMethod method, Compile compile, CompileClass component, StringBuffer setMethodBody) {
        return AnnotationUtils.getAnnotation(component.getClasses(),method, AnnotationNames.ListenEvent)
                .map(listenEvent -> {
                    CtClass eventClass = AnnotationUtils.getAnnotationValue(listenEvent,"value")
                            .map(value -> (ClassMemberValue) value)
                            .map(ClassMemberValue::getValue)
                            .map(className -> JavassistUtils.get(pool,className))
                            .orElseThrow(RuntimeException::new);

                    CtClass invoker = pool.makeClass(String.format(
                            "%s.event.invoker.MethodEventInvoker%s",
                            compile.getPackageName(),
                            UUID.randomUUID().toString().replaceAll("-","")));

                    try {
                        invoker.setSuperclass(pool.get(ClassNames.AbstractMethodEventInvoker));
                        this.invokeMethod(
                                invoker,
                                component.getClasses(),
                                method,
                                eventClass,
                                pool,
                                false
                        );
                        setMethodBody.append(String.format(
                                "$0.registerMethodListener($0.bundleContext,(%s)new %s(%s.class,$0.applicationContext));\n",
                                ClassNames.AbstractMethodEventInvoker,
                                invoker.getName(),
                                eventClass.getName()
                        ));

                        return invoker;
                    } catch (CannotCompileException | NotFoundException e) {
                        e.printStackTrace();
                    }
                    return null;
                });
    }

    private Optional<CtClass> getMethodNativeInvoker(ClassPool pool, CtMethod method, Compile compile, CompileClass component, StringBuffer setMethodBody) {
        return AnnotationUtils.getAnnotation(component.getClasses(),method, AnnotationNames.ListenNativeEvent)
                .map(listenEvent -> {
                    String listenValue = AnnotationUtils.getAnnotationValue(listenEvent, "value")
                            .map(value -> (ClassMemberValue) value)
                            .map(ClassMemberValue::getValue)
                            .orElseThrow(RuntimeException::new);

                    CtClass invoker = pool.makeClass(String.format(
                            "%s.event.invoker.MethodEventInvoker%s",
                            compile.getPackageName(),
                            UUID.randomUUID().toString().replaceAll("-", "")));

                    try {
                        invoker.setSuperclass(pool.get(ClassNames.AbstractMethodNativeEventInvoker));
                        this.invokeMethod(
                                invoker,
                                component.getClasses(),
                                method,
                                pool.get(ClassNames.NativeEvent),
                                pool,
                                true
                        );
                        setMethodBody.append(String.format(
                                "$0.registerNativeMethodListener($0.bundleContext,(%s)new %s(\"%s\",$0.applicationContext));\n",
                                ClassNames.AbstractMethodEventInvoker,
                                invoker.getName(),
                                listenValue
                        ));

                        return invoker;
                    } catch (CannotCompileException | NotFoundException e) {
                        e.printStackTrace();
                    }
                    return null;
                });
    }
}
