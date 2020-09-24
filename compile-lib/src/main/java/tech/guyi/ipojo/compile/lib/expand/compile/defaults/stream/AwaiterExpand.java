package tech.guyi.ipojo.compile.lib.expand.compile.defaults.stream;

import javassist.*;
import javassist.bytecode.annotation.BooleanMemberValue;
import tech.guyi.ipojo.compile.lib.compile.entry.CompileClass;
import tech.guyi.ipojo.compile.lib.configuration.Compile;
import tech.guyi.ipojo.compile.lib.cons.AnnotationNames;
import tech.guyi.ipojo.compile.lib.cons.ClassNames;
import tech.guyi.ipojo.compile.lib.expand.compile.CompileExpand;
import tech.guyi.ipojo.compile.lib.expand.compile.defaults.stream.entry.FieldEntry;
import tech.guyi.ipojo.compile.lib.utils.AnnotationUtils;
import tech.guyi.ipojo.compile.lib.utils.JavassistUtils;

import java.util.Set;

public class AwaiterExpand implements CompileExpand {

    @Override
    public boolean check(Compile compile) {
        return compile.getModules().contains("stream");
    }

    private CtMethod getAfterPropertiesSetMethod(CtClass classes, CtClass initializingBean) throws CannotCompileException {
        CtMethod method = null;

        try {
            if (classes.subtypeOf(initializingBean)){
                method = classes.getDeclaredMethod("afterPropertiesSet",new CtClass[0]);
            }
        } catch (NotFoundException ignored){}

        if (method == null){
            classes.addInterface(initializingBean);
            method = new CtMethod(CtClass.voidType,"afterPropertiesSet",new CtClass[0],classes);
            method.setBody("{}");
            classes.addMethod(method);
        }

        return method;
    }

    @Override
    public Set<CompileClass> execute(ClassPool pool, Compile compile, Set<CompileClass> components) throws Exception {
        CtClass object = pool.get(Object.class.getName());
        CtClass initializingBean = pool.get(ClassNames.InitializingBean);
        components
                .stream()
                .map(component -> JavassistUtils.getFields(object,component.getClasses(), field -> {
                    if (field.hasAnnotation(AnnotationNames.Awaiter)){
                        boolean sync = AnnotationUtils.getAnnotationValue(component.getClasses(),field,AnnotationNames.Awaiter,"sync")
                                .map(value -> (BooleanMemberValue) value)
                                .map(BooleanMemberValue::getValue)
                                .orElse(false);
                        return new FieldEntry(sync,component.getClasses(),field);
                    }
                    return null;
                }))
                .filter(fields -> !fields.isEmpty())
                .forEach(fields -> {
                    StringBuffer setCode = new StringBuffer();
                    CtClass fluxAwaiter = JavassistUtils.get(pool,ClassNames.FluxAwaiter);
                    fields.forEach(field -> setCode.append(
                            String.format(
                                    "$0.%s = %s.create(%s);\n",
                                    field.getField().getName(),
                                    JavassistUtils.equalsType(field.getField(),fluxAwaiter) ? ClassNames.FluxAwaiter : ClassNames.MonoAwaiter,
                                    field.isSync()
                            )
                    ));
                    try {
                        CtMethod method = this.getAfterPropertiesSetMethod(fields.get(0).getClasses(),initializingBean);
                        method.insertBefore(setCode.toString());
                    } catch (CannotCompileException e) {
                        e.printStackTrace();
                    }
                });
        return components;
    }

}
