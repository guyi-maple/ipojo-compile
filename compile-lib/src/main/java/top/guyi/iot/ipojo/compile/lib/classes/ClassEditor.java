package top.guyi.iot.ipojo.compile.lib.classes;

import javassist.bytecode.annotation.BooleanMemberValue;
import javassist.bytecode.annotation.StringMemberValue;
import top.guyi.iot.ipojo.compile.lib.cons.AnnotationNames;
import top.guyi.iot.ipojo.compile.lib.cons.ClassNames;
import top.guyi.iot.ipojo.compile.lib.utils.AnnotationUtils;
import top.guyi.iot.ipojo.compile.lib.utils.StringUtils;
import top.guyi.iot.ipojo.compile.lib.classes.entry.FieldEntry;
import top.guyi.iot.ipojo.compile.lib.configuration.Compile;
import top.guyi.iot.ipojo.compile.lib.expand.inject.FieldInjector;
import top.guyi.iot.ipojo.compile.lib.expand.inject.FieldInjectorFactory;
import top.guyi.iot.ipojo.compile.lib.utils.JavassistUtils;
import javassist.*;

import java.util.*;

public class ClassEditor {

    private final FieldInjectorFactory injectorFactory = new FieldInjectorFactory();

    private CtClass applicationClass;
    private CtClass componentInterfaceClass;
    private CtClass getApplicationClass(ClassPool pool) throws NotFoundException {
        if (this.applicationClass == null){
            this.applicationClass = pool.get(ClassNames.ApplicationContext);
        }
        return this.applicationClass;
    }
    private CtClass getComponentInterfaceClass(ClassPool pool) throws NotFoundException {
        if (this.componentInterfaceClass == null){
            this.componentInterfaceClass = pool.get(ClassNames.ComponentInterface);
        }
        return this.componentInterfaceClass;
    }

    public String getInjectMethodBody(ClassPool pool,CtClass classes, Compile compile) throws NotFoundException {
        StringBuilder sb = new StringBuilder("{");
        JavassistUtils.getFields(
                pool.get(Object.class.getName()),
                classes,field ->
                        AnnotationUtils.getAnnotation(classes, field,AnnotationNames.Resource)
                                .map(annotation -> new FieldEntry(
                                        field,
                                        AnnotationUtils.getAnnotationValue(annotation,"name")
                                                .map(name -> (StringMemberValue) name)
                                                .map(StringMemberValue::getValue)
                                                .orElse(""),
                                        AnnotationUtils.getAnnotationValue(annotation,"equals")
                                                .map(equals -> (BooleanMemberValue) equals)
                                                .map(BooleanMemberValue::getValue)
                                                .orElse(false)))
                                .orElseGet(() -> {
                                    try {
                                        javax.annotation.Resource r = (javax.annotation.Resource) field.getAnnotation(javax.annotation.Resource.class);
                                        if (r != null){
                                            if (StringUtils.isEmpty(r.name())){
                                                return new FieldEntry(field,false);
                                            }else{
                                                return new FieldEntry(field,r.name(),false);
                                            }
                                        }
                                    } catch (ClassNotFoundException e) {
                                        e.printStackTrace();
                                    }
                                    return null;
                                })
        ).stream()
                .map(field -> {
                    CtMethod setMethod = JavassistUtils.getSetMethod(classes,field.getField());
                    FieldInjector injector = injectorFactory.get(field,pool);

                    compile.addUseComponent(field.getField());

                    return injector.injectCode(field,setMethod,pool);
                })
                .filter(Objects::nonNull)
                .forEach(sb::append);

        sb.append("}");
        return sb.toString();
    }

    public void addInjectMethod(ClassPool pool, CtClass classes, Compile compile) throws NotFoundException, CannotCompileException {
        try {
            JavassistUtils.getInjectMethod(pool,classes);
        } catch (NotFoundException e) {
            classes.addInterface(this.getComponentInterfaceClass(pool));
            CtMethod method = new CtMethod(CtClass.voidType,"inject",new CtClass[]{this.getApplicationClass(pool)},classes);
            method.setBody(this.getInjectMethodBody(pool,classes,compile));
            classes.addMethod(method);
        }
    }

}
