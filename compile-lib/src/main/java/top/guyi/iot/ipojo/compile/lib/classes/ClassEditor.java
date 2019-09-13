package top.guyi.iot.ipojo.compile.lib.classes;

import top.guyi.iot.ipojo.application.ApplicationContext;
import top.guyi.iot.ipojo.application.annotation.Resource;
import top.guyi.iot.ipojo.application.component.ComponentInterface;
import top.guyi.iot.ipojo.compile.lib.utils.JavassistUtils;
import javassist.*;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ClassEditor {

    private CtClass applicationClass;
    private CtClass componentInterfaceClass;
    private CtClass getApplicationClass(ClassPool pool) throws NotFoundException {
        if (this.applicationClass == null){
            this.applicationClass = pool.get(ApplicationContext.class.getName());
        }
        return this.applicationClass;
    }
    private CtClass getComponentInterfaceClass(ClassPool pool) throws NotFoundException {
        if (this.componentInterfaceClass == null){
            this.componentInterfaceClass = pool.get(ComponentInterface.class.getName());
        }
        return this.componentInterfaceClass;
    }

    public List<CtField> getFields(CtClass classes){
        return Arrays.stream(classes.getDeclaredFields())
                .filter(field -> field.hasAnnotation(Resource.class))
                .collect(Collectors.toList());
    }

    public String getInjectMethodBody(CtClass classes){
        StringBuffer sb = new StringBuffer("{");
        this.getFields(classes)
                .stream()
                .map(field -> {
                    CtMethod setMethod = JavassistUtils.getSetMethod(classes,field);
                    try{
                        return String.format(
                                "$0.%s((%s) $1.get(%s.class));",
                                setMethod.getName(),
                                field.getType().getName(),
                                field.getType().getName()
                        );
                    }catch (NotFoundException e){
                        e.printStackTrace();
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .forEach(sb::append);
        sb.append("}");
        return sb.toString();
    }

    public void addInjectMethod(ClassPool pool,CtClass classes) throws CannotCompileException, NotFoundException {
        try{
            JavassistUtils.getInjectMethod(pool,classes);
        }catch (NotFoundException e){
            classes.addInterface(this.getComponentInterfaceClass(pool));
            CtMethod method = new CtMethod(CtClass.voidType,"inject",new CtClass[]{this.getApplicationClass(pool)},classes);
            method.setBody(this.getInjectMethodBody(classes));
            classes.addMethod(method);
        }
    }

}
