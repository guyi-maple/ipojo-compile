package top.guyi.iot.ipojo.compile.lib.expand.compile.defaults.service;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.bytecode.annotation.ClassMemberValue;
import top.guyi.iot.ipojo.compile.lib.compile.entry.CompileClass;
import top.guyi.iot.ipojo.compile.lib.configuration.Compile;
import top.guyi.iot.ipojo.compile.lib.cons.AnnotationNames;
import top.guyi.iot.ipojo.compile.lib.cons.ClassNames;
import top.guyi.iot.ipojo.compile.lib.enums.CompileType;
import top.guyi.iot.ipojo.compile.lib.expand.compile.CompileExpand;
import top.guyi.iot.ipojo.compile.lib.utils.AnnotationUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author guyi
 * 服务注册拓展
 */
public class ServiceRegisterExpand implements CompileExpand {

    @Override
    public boolean check(Compile compile) {
        return compile.getType() == CompileType.BUNDLE;
    }

    @Override
    public Set<CompileClass> execute(ClassPool pool, Compile compile, Set<CompileClass> components) throws Exception {
        List<CompileClass> services = components.stream()
                .filter(component -> AnnotationUtils.getAnnotation(component.getClasses(), AnnotationNames.Service).isPresent())
                .collect(Collectors.toList());

        if (!services.isEmpty()){
            CtClass register = pool.makeClass(String.format("%s.service.DefaultAutoServiceRegister",compile.getPackageName()));
            register.setSuperclass(pool.get(ClassNames.AbstractServiceRegister));
            components.add(new CompileClass(register,true,true,false));

            compile.addUseComponent(register);

            CtMethod method = new CtMethod(CtClass.voidType,"registerAll",new CtClass[0],register);
            method.setModifiers(Modifier.PROTECTED);
            StringBuffer methodBody = new StringBuffer("{");

            services.forEach(component ->
                    AnnotationUtils.getAnnotationValue(component.getClasses(),AnnotationNames.Service,"value")
                            .ifPresent(value -> methodBody.append(String.format(
                                    "$0.register(new %s(%s.class,%s.class));",
                                    ClassNames.ServiceEntry,
                                    ((ClassMemberValue) value).getValue(),
                                    component.getClasses().getName()))));

            methodBody.append("}");
            method.setBody(methodBody.toString());
            register.addMethod(method);
        }

        return components;
    }

}
