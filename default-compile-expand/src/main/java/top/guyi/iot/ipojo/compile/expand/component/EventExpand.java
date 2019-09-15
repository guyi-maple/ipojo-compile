package top.guyi.iot.ipojo.compile.expand.component;

import javassist.*;
import top.guyi.iot.ipojo.application.ApplicationContext;
import top.guyi.iot.ipojo.application.osgi.event.interfaces.defaults.DefaultEventConverter;
import top.guyi.iot.ipojo.application.osgi.event.EventPublisher;
import top.guyi.iot.ipojo.application.osgi.event.EventRegister;
import top.guyi.iot.ipojo.application.osgi.event.annotation.ListenEvent;
import top.guyi.iot.ipojo.application.osgi.event.interfaces.Event;
import top.guyi.iot.ipojo.application.osgi.event.interfaces.EventConverter;
import top.guyi.iot.ipojo.application.osgi.event.interfaces.EventListener;
import top.guyi.iot.ipojo.application.osgi.event.invoker.MethodEventInvoker;
import top.guyi.iot.ipojo.compile.lib.compile.entry.CompileClass;
import top.guyi.iot.ipojo.compile.lib.configuration.Compile;
import top.guyi.iot.ipojo.compile.lib.expand.CompileExpand;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class EventExpand implements CompileExpand {

    private List<CtClass> getConverter(ClassPool pool, Set<CompileClass> components) throws NotFoundException {
        CtClass converterClass = pool.get(EventConverter.class.getName());
        return components.stream()
                .filter(component -> {
                    try {
                        return component.getClasses().subtypeOf(converterClass);
                    } catch (NotFoundException e) {
                        e.printStackTrace();
                    }
                    return false;
                })
                .map(CompileClass::getClasses)
                .collect(Collectors.toList());
    }

    @Override
    public Set<CompileClass> execute(ClassPool pool, Compile compile, Set<CompileClass> components) throws Exception {
        CtClass register = pool.makeClass(String.format("%s.DefaultEventRegister", compile.getPackageName()));
        register.setSuperclass(pool.get(EventRegister.class.getName()));
        components.add(new CompileClass(register));

        this.setConverter(register,pool,components);
        this.setEventListeners(register,pool,components);
        this.setMethodEventListeners(register,pool,components, compile);
        this.setPublisher(pool,components, compile);
        return components;
    }

    private void setPublisher(ClassPool pool, Set<CompileClass> components, Compile compile) throws NotFoundException, CannotCompileException {
        List<CtClass> converters = this.getConverter(pool,components);
        CtClass superClass = pool.get(EventPublisher.class.getName());
        CtClass publisher = pool.makeClass(String.format("%s.AutoEventPublisher", compile.getPackageName()));
        publisher.setSuperclass(superClass);
        CtMethod setMethod = new CtMethod(CtClass.voidType,"setAllEventConverter",new CtClass[0],publisher);
        setMethod.setModifiers(Modifier.PROTECTED);
        publisher.addMethod(setMethod);

        StringBuffer setMethodBody = new StringBuffer("{");
        converters.forEach(converter -> setMethodBody.append(String.format(
                "$0.addConverter((%s)$0.applicationContext.get(%s.class,true));\n",
                converter.getName(),
                converter.getName()
        )));
        setMethodBody.append(String.format(
                "$0.addConverter(new %s());\n",
                DefaultEventConverter.class.getName()
        ));
        setMethodBody.append("}");

        setMethod.setBody(setMethodBody.toString());

        components.add(new CompileClass(publisher));
    }

    private void setConverter(CtClass register,ClassPool pool,Set<CompileClass> components) throws NotFoundException, CannotCompileException {
        List<CtClass> converters = this.getConverter(pool,components);
        CtMethod setMethod = new CtMethod(CtClass.voidType,"setAllConverter",new CtClass[0],register);
        setMethod.setModifiers(Modifier.PROTECTED);
        register.addMethod(setMethod);

        StringBuffer setMethodBody = new StringBuffer("{");
        converters.forEach(converter -> setMethodBody.append(String.format(
                "$0.setConverter((%s)$0.applicationContext.get(%s.class,true));\n",
                converter.getName(),
                converter.getName()
        )));
        setMethodBody.append(String.format(
                "$0.setConverter(new %s());\n",
                DefaultEventConverter.class.getName()
        ));
        setMethodBody.append("}");

        setMethod.setBody(setMethodBody.toString());
    }

    private void setMethodEventListeners(CtClass register, ClassPool pool, Set<CompileClass> components, Compile compile) throws NotFoundException, CannotCompileException, ClassNotFoundException {
        CtMethod setMethod = new CtMethod(CtClass.voidType,"registerAllMethodListener",new CtClass[0],register);
        setMethod.setModifiers(Modifier.PROTECTED);
        register.addMethod(setMethod);

        StringBuffer setMethodBody = new StringBuffer("{");
        Set<CompileClass> tmp = new HashSet<>(components);
        for (CompileClass component : tmp) {
            for (CtMethod method : component.getClasses().getDeclaredMethods()) {
                ListenEvent listenEvent = (ListenEvent) method.getAnnotation(ListenEvent.class);
                if (listenEvent != null){
                    CtClass eventClass = pool.get(listenEvent.value().getName());
                    CtClass invoker = pool.makeClass(String.format(
                            "%s.MethodEventInvoker%s",
                            compile.getPackageName(),
                            UUID.randomUUID().toString().replaceAll("-","")));
                    invoker.setSuperclass(pool.get(MethodEventInvoker.class.getName()));
                    this.invokeMethod(
                            invoker,
                            component.getClasses(),
                            method,
                            eventClass,
                            pool);
                    setMethodBody.append(String.format(
                            "$0.registerMethodListener($0.bundleContext,(%s)new %s(%s.class,$0.applicationContext));\n",
                            MethodEventInvoker.class.getName(),
                            invoker.getName(),
                            eventClass.getName()
                    ));

                    components.add(new CompileClass(false,invoker));
                }
            }
        }
        setMethodBody.append("}");

        setMethod.setBody(setMethodBody.toString());
    }

    private void invokeMethod(CtClass invoker,CtClass bean,CtMethod method,CtClass eventClass,ClassPool pool) throws NotFoundException, CannotCompileException {
        CtConstructor constructor = new CtConstructor(new CtClass[]{
                pool.get(Class.class.getName()),
                pool.get(ApplicationContext.class.getName())
        },invoker);
        constructor.setBody("{super($1,$2);}");
        invoker.addConstructor(constructor);

        CtMethod invokeMethod = new CtMethod(CtClass.voidType,"invoke",new CtClass[]{
                pool.get(ApplicationContext.class.getName()),
                pool.get(Event.class.getName())
        },invoker);
        invokeMethod.setModifiers(Modifier.PROTECTED);
        invoker.addMethod(invokeMethod);

        StringBuffer argsBody = new StringBuffer();
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

        CtClass listenerClass = pool.get(EventListener.class.getName());
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

}
