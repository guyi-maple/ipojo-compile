package top.guyi.iot.ipojo.compile.lib.expand.compile.defaults.service;

import org.apache.commons.codec.digest.DigestUtils;
import top.guyi.iot.ipojo.application.ApplicationContext;
import top.guyi.iot.ipojo.compile.lib.compile.entry.CompileClass;
import top.guyi.iot.ipojo.compile.lib.configuration.Compile;
import top.guyi.iot.ipojo.compile.lib.enums.CompileType;
import top.guyi.iot.ipojo.compile.lib.expand.compile.CompileExpand;
import javassist.*;
import org.osgi.framework.BundleContext;
import top.guyi.iot.ipojo.application.osgi.service.reference.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class BundleServiceReferenceExpand implements CompileExpand {

    @Override
    public int order() {
        return 1000;
    }

    @Override
    public boolean check(Compile compile) {
        return compile.getType() == CompileType.BUNDLE;
    }

    @Override
    public Set<CompileClass> execute(ClassPool pool, Compile compile, Set<CompileClass> components) throws Exception {
        StringBuilder methodBody = new StringBuilder("{\n");

        Set<CompileClass> add = new HashSet<>();
        for (CompileClass component : components) {
            for (CtMethod method : this.getMethods(component.getClasses())) {
                BundleServiceReference reference = (BundleServiceReference) method.getAnnotation(BundleServiceReference.class);

                String checker = "null";
                if (reference.checker() != BundleServiceReferenceChecker.class){
                    checker = String.format("$1.get(%s.class,true)",reference.checker().getName());
                }

                methodBody.append(String.format(
                        "$0.register(new %s(%s,%s,%s));\n",
                        ServiceReferenceEntry.class.getName(),
                        this.getClassString(reference),
                        this.invokerMethod(pool,add,component.getClasses(),method, compile),
                        checker
                ));
            }
        }

        methodBody.append("}");

        CtClass listener = pool.makeClass(String.format("%s.AutoDefaultBundleServiceListener", compile.getPackageName()));
        listener.setSuperclass(pool.get(AbstractBundleServiceListener.class.getName()));

        CtMethod registerAll = new CtMethod(CtClass.voidType,"registerAll",new CtClass[]{
                pool.get(ApplicationContext.class.getName())
        },listener);

        registerAll.setBody(methodBody.toString());

        listener.addMethod(registerAll);

        components.add(new CompileClass("AutoDefaultBundleServiceListener",listener,true,true,false,1000));
        components.addAll(add);

        return components;
    }

    private List<CtMethod> getMethods(CtClass component){
        CtMethod[] ms = component.getMethods();
        return Arrays.stream(component.getMethods())
                .filter(method -> method.hasAnnotation(BundleServiceReference.class))
                .collect(Collectors.toList());
    }

    private String getClassString(BundleServiceReference reference){
        StringBuilder sb = new StringBuilder("new java.lang.Class[]{");
        for (Class<?> classes : reference.value()) {
            sb.append(classes.getName());
            sb.append(".class,");
        }
        return sb.substring(0,sb.length() - 1) + "}";
    }

    private String invokerMethod(ClassPool pool,Set<CompileClass> add,CtClass component, CtMethod method, Compile compile) throws NotFoundException, CannotCompileException, IOException {
        String className = String.format("%s.ServiceReferenceInvoker%s", compile.getPackageName(), DigestUtils.md5Hex(UUID.randomUUID().toString()));

        CtClass invoker = pool.makeClass(className);
        invoker.setSuperclass(pool.get(ServiceReferenceInvoker.class.getName()));
        CtMethod invoke = new CtMethod(CtClass.voidType,"invoke",new CtClass[]{
                pool.get(ServiceReferenceEntry.class.getName()),
                pool.get(ApplicationContext.class.getName()),
                pool.get(BundleContext.class.getName())
        },invoker);

        StringBuilder sb = new StringBuilder();
        for (CtClass type : method.getParameterTypes()) {
            sb.append(String.format("(%s)$0.get(%s.class,$2,$3),",type.getName(),type.getName()));
        }

        invoke.setBody(String.format(
                "{((%s)$2.get(%s.class,true)).%s(%s);}",
                component.getName(),
                component.getName(),
                method.getName(),
                method.getParameterTypes().length > 0 ? sb.substring(0,sb.length() - 1) : "")
        );
        invoker.addMethod(invoke);

        invoker.writeFile(compile.getProject().getOutput());
        add.add(new CompileClass(false,invoker));
        return String.format("new %s()",className);
    }

}