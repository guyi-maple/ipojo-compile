package top.guyi.iot.ipojo.compile.lib.expand.compile.defaults.stream;

import javassist.*;
import top.guyi.iot.ipojo.application.bean.interfaces.InitializingBean;
import top.guyi.iot.ipojo.compile.lib.compile.entry.CompileClass;
import top.guyi.iot.ipojo.compile.lib.configuration.Compile;
import top.guyi.iot.ipojo.compile.lib.expand.compile.CompileExpand;
import top.guyi.iot.ipojo.compile.lib.expand.compile.defaults.stream.entry.FieldEntry;
import top.guyi.iot.ipojo.compile.lib.utils.JavassistUtils;
import top.guyi.iot.ipojo.module.stream.annotation.Awaiter;
import top.guyi.iot.ipojo.module.stream.async.awaiter.MonoAwaiter;

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
        CtClass initializingBean = pool.get(InitializingBean.class.getName());
        components
                .stream()
                .map(component -> JavassistUtils.getFields(object,component.getClasses(), field -> {
                    if (field.hasAnnotation(Awaiter.class)){
                        boolean sync = false;
                        try {
                            Awaiter awaiter = (Awaiter) field.getAnnotation(Awaiter.class);
                            sync = awaiter.sync();
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                        return new FieldEntry(sync,component.getClasses(),field);
                    }
                    return null;
                }))
                .filter(fields -> !fields.isEmpty())
                .forEach(fields -> {
                    StringBuffer setCode = new StringBuffer();
                    fields.forEach(field -> setCode.append(
                            String.format(
                                    "$0.%s = %s.create(%s);\n",
                                    field.getField().getName(),
                                    MonoAwaiter.class.getName(),
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
