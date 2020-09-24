package top.guyi.ipojo.compile.lib.expand.compile.defaults.service;

import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.ClassMemberValue;
import lombok.AllArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import top.guyi.ipojo.compile.lib.compile.entry.CompileClass;
import top.guyi.ipojo.compile.lib.configuration.Compile;
import top.guyi.ipojo.compile.lib.cons.AnnotationNames;
import top.guyi.ipojo.compile.lib.cons.ClassNames;
import top.guyi.ipojo.compile.lib.enums.CompileType;
import top.guyi.ipojo.compile.lib.expand.compile.CompileExpand;
import javassist.*;
import top.guyi.ipojo.compile.lib.utils.AnnotationUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@AllArgsConstructor
class BundleServiceReferenceEntry {
    CompileClass component;
    CtMethod method;
}

/**
 * @author guyi
 * 服务获取拓展
 */
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
        List<BundleServiceReferenceEntry> entries = new LinkedList<>();
        Set<CompileClass> tmpComponents = compile.filterUseComponents(components)
                .stream()
                .filter(CompileClass::isComponent)
                .collect(Collectors.toSet());
        for (CompileClass component : tmpComponents) {
            component.getClasses().freeze();
            for (CtMethod method : this.getMethods(component.getClasses())) {
                entries.add(new BundleServiceReferenceEntry(component,method));
            }
        }

        if (!entries.isEmpty()){
            StringBuilder methodBody = new StringBuilder("{\n");
            Set<CompileClass> add = new HashSet<>();
            for (BundleServiceReferenceEntry entry : entries) {
                Annotation reference = AnnotationUtils.getAnnotation(entry.component.getClasses(),entry.method, AnnotationNames.BundleServiceReference).orElse(null);
                if (reference != null){
                    String checker = AnnotationUtils.getAnnotationValue(reference,"checker")
                            .map(value -> ((ClassMemberValue) value).getValue())
                            .filter(value -> !ClassNames.BundleServiceReferenceChecker.equals(value))
                            .map(value -> String.format("$1.get(%s.class,true)",value))
                            .orElse("null");

                    methodBody.append(String.format(
                            "$0.register(new %s(%s,%s,%s));\n",
                            ClassNames.ServiceReferenceEntry,
                            this.getClassString(reference),
                            this.invokerMethod(pool,add,entry.component.getClasses(),entry.method, compile),
                            checker
                    ));
                }
            }

            methodBody.append("}");

            CtClass listener = pool.makeClass(String.format("%s.service.AutoDefaultBundleServiceListener", compile.getPackageName()));
            listener.setSuperclass(pool.get(ClassNames.AbstractBundleServiceListener));
            compile.addUseComponent(listener);

            CtMethod registerAll = new CtMethod(CtClass.voidType,"registerAll",new CtClass[]{
                    pool.get(ClassNames.ApplicationContext)
            },listener);

            registerAll.setBody(methodBody.toString());

            listener.addMethod(registerAll);

            components.add(new CompileClass("AutoDefaultBundleServiceListener",listener,true,true,false,1000));
            components.addAll(add);
        }

        return components;
    }

    private List<CtMethod> getMethods(CtClass component){
        return Arrays.stream(component.getMethods())
                .filter(method -> AnnotationUtils.getAnnotation(component,method,AnnotationNames.BundleServiceReference).isPresent())
                .collect(Collectors.toList());
    }

    private String getClassString(Annotation reference){
        StringBuilder sb = new StringBuilder("new java.lang.Class[]{");
        AnnotationUtils.getAnnotationValues(reference,"value")
                .stream()
                .map(value -> ((ClassMemberValue) value).getValue())
                .forEach(value -> sb.append(value).append(".class,"));
        return sb.substring(0,sb.length() - 1) + "}";
    }

    private String invokerMethod(ClassPool pool,Set<CompileClass> add,CtClass component, CtMethod method, Compile compile) throws NotFoundException, CannotCompileException, IOException {
        String className = String.format("%s.service.invoker.ServiceReferenceInvoker%s", compile.getPackageName(), DigestUtils.md5Hex(UUID.randomUUID().toString()));

        CtClass invoker = pool.makeClass(className);
        invoker.setSuperclass(pool.get(ClassNames.AbstractServiceReferenceInvoker));
        CtMethod invoke = new CtMethod(CtClass.voidType,"invoke",new CtClass[]{
                pool.get(ClassNames.ServiceReferenceEntry),
                pool.get(ClassNames.ApplicationContext),
                pool.get(ClassNames.BundleContext)
        },invoker);

        compile.addUseComponent(invoker);

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
