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
import java.util.stream.Collectors;

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

    private Map<String,CtField> listField(CtClass stopClass,CtClass classes,Map<String,CtField> fields){
        List<CtField> fieldList = new LinkedList<>();
        fieldList.addAll(Arrays.asList(classes.getDeclaredFields()));
        fieldList.addAll(Arrays.asList(classes.getFields()));
        fieldList
                .stream()
                .filter(field -> !fields.containsKey(field.getName()))
                .forEach(field -> fields.put(field.getName(),field));

        try {
            CtClass superClass = classes.getSuperclass();
            if (!superClass.getName().equals(stopClass.getName())){
                return this.listField(stopClass,superClass,fields);
            }
        } catch (NotFoundException e) {
            e.printStackTrace();
        }

        return fields;
    }

    public List<FieldEntry> getFields(CtClass stopClass,CtClass classes){
        return this.listField(stopClass,classes,new HashMap<>()).values()
                .stream()
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

    public String getInjectMethodBody(ClassPool pool,CtClass classes) throws NotFoundException {
        StringBuffer sb = new StringBuffer("{");
        this.getFields(pool.get(Object.class.getName()),classes)
                .stream()
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
