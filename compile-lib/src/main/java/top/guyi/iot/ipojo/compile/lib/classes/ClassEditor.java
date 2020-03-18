package top.guyi.iot.ipojo.compile.lib.classes;

import top.guyi.iot.ipojo.application.ApplicationContext;
import top.guyi.iot.ipojo.application.annotation.Resource;
import top.guyi.iot.ipojo.application.component.ComponentInterface;
import top.guyi.iot.ipojo.application.utils.StringUtils;
import top.guyi.iot.ipojo.compile.lib.classes.entry.FieldEntry;
import top.guyi.iot.ipojo.compile.lib.expand.inject.FieldInjector;
import top.guyi.iot.ipojo.compile.lib.expand.inject.FieldInjectorFactory;
import top.guyi.iot.ipojo.compile.lib.utils.JavassistUtils;
import javassist.*;

import java.util.*;

public class ClassEditor {

    private FieldInjectorFactory injectorFactory = new FieldInjectorFactory();

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

    public String getInjectMethodBody(ClassPool pool,CtClass classes) throws NotFoundException {
        StringBuilder sb = new StringBuilder("{");
        List<FieldEntry> fields = JavassistUtils.getFields(pool.get(Object.class.getName()),classes,field -> {
            try {
                Resource resource = (Resource) field.getAnnotation(Resource.class);
                if (resource != null){
                    if (StringUtils.isEmpty(resource.name())){
                        return new FieldEntry(field,resource.equals());
                    }else{
                        return new FieldEntry(field,resource.name(),resource.equals());
                    }
                }
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
        });
        fields.stream()
                .map(field -> {
                    CtMethod setMethod = JavassistUtils.getSetMethod(classes,field.getField());
                    FieldInjector injector = injectorFactory.get(field,pool);
                    return injector.injectCode(field,setMethod,pool);
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
            method.setBody(this.getInjectMethodBody(pool,classes));
            classes.addMethod(method);
        }
    }

}
