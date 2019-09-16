package top.guyi.iot.ipojo.compile.lib.classes;

import top.guyi.iot.ipojo.application.ApplicationContext;
import top.guyi.iot.ipojo.application.annotation.Resource;
import top.guyi.iot.ipojo.application.component.ComponentInterface;
import top.guyi.iot.ipojo.application.utils.StringUtils;
import top.guyi.iot.ipojo.compile.lib.classes.entry.FieldEntry;
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

    public List<FieldEntry> getFields(CtClass classes){
        return Arrays.stream(classes.getDeclaredFields())
                .map(field -> {
                    try {
                        Resource resource = (Resource) field.getAnnotation(Resource.class);
                        if (resource != null){
                            if (StringUtils.isEmpty(resource.name())){
                                return new FieldEntry(field,resource.equals());
                            }else{
                                return new FieldEntry(field,resource.name(),resource.equals());
                            }
                        }
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public String getInjectMethodBody(CtClass classes){
        StringBuffer sb = new StringBuffer("{");
        this.getFields(classes)
                .stream()
                .map(field -> {
                    CtMethod setMethod = JavassistUtils.getSetMethod(classes,field.getField());
                    try{
                        if ((!field.isEquals())
                                || (field.getField().getType().isInterface())
                                || (Modifier.isAbstract(field.getField().getType().getModifiers()))){
                            return String.format(
                                    "$0.%s((%s) $1.get(%s.class));",
                                    setMethod.getName(),
                                    field.getField().getType().getName(),
                                    field.getField().getType().getName()
                            );
                        }

                        if (StringUtils.isEmpty(field.getName())){
                            return String.format(
                                    "$0.%s((%s) $1.get(%s.class,true));",
                                    setMethod.getName(),
                                    field.getField().getType().getName(),
                                    field.getField().getType().getName()
                            );
                        }

                        return String.format(
                                "$0.%s((%s) $1.get(%s.class,\"%s\"));",
                                setMethod.getName(),
                                field.getField().getType().getName(),
                                field.getField().getType().getName(),
                                field.getName()
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
