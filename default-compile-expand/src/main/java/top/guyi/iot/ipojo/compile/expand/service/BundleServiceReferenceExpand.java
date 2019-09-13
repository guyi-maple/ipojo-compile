package top.guyi.iot.ipojo.compile.expand.service;

import top.guyi.iot.ipojo.application.ApplicationContext;
import top.guyi.iot.ipojo.compile.lib.compile.CompileExecutor;
import top.guyi.iot.ipojo.compile.lib.compile.entry.CompileInfo;
import top.guyi.iot.ipojo.compile.lib.expand.CompileExpand;
import javassist.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.osgi.framework.BundleContext;
import top.guyi.iot.ipojo.application.osgi.service.reference.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class BundleServiceReferenceExpand implements CompileExpand {

    @Override
    public int order() {
        return 1000;
    }

    @Override
    public Set<CtClass> execute(ClassPool pool, String path, CompileInfo compileInfo, Set<CtClass> components) throws Exception {
        StringBuilder methodBody = new StringBuilder("{\n");

        for (CtClass component : components) {
            for (CtMethod method : this.getMethods(component)) {
                BundleServiceReference reference = (BundleServiceReference) method.getAnnotation(BundleServiceReference.class);

                String checker = "null";
                if (reference.checker() != BundleServiceReferenceChecker.class){
                    checker = String.format("$1.get(%s.class,true)",reference.checker().getName());
                }

                methodBody.append(String.format(
                        "$0.register(new %s(%s,%s,%s));\n",
                        ServiceReferenceEntry.class.getName(),
                        this.getClassString(reference),
                        this.invokerMethod(pool,component,method,compileInfo),
                        checker
                ));
            }
        }

        methodBody.append("}");

        CtClass listener = pool.makeClass(String.format("%s.AutoDefaultBundleServiceListener",compileInfo.getPackageName()));
        listener.setSuperclass(pool.get(AbstractBundleServiceListener.class.getName()));

        CtMethod registerAll = new CtMethod(CtClass.voidType,"registerAll",new CtClass[]{
                pool.get(ApplicationContext.class.getName())
        },listener);

        registerAll.setBody(methodBody.toString());

        listener.addMethod(registerAll);

        components.add(listener);

        return components;
    }

    private List<CtMethod> getMethods(CtClass component){
        CtMethod[] ms = component.getDeclaredMethods();
        return Arrays.stream(component.getDeclaredMethods())
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

    private String invokerMethod(ClassPool pool,CtClass component,CtMethod method,CompileInfo compileInfo) throws NotFoundException, CannotCompileException, IOException {
        String className = String.format("%s.Invoker%s",compileInfo.getPackageName(), DigestUtils.md5Hex(UUID.randomUUID().toString()));

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
        invoker.writeFile(compileInfo.getOutput());

        return String.format("new %s()",className);
    }

}
