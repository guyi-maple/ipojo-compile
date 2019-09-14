package top.guyi.iot.ipojo.compile.expand.service;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.Modifier;
import top.guyi.iot.ipojo.application.osgi.service.ServiceRegister;
import top.guyi.iot.ipojo.application.osgi.service.annotation.Service;
import top.guyi.iot.ipojo.application.osgi.service.entry.ServiceEntry;
import top.guyi.iot.ipojo.compile.lib.compile.entry.CompileClass;
import top.guyi.iot.ipojo.compile.lib.configuration.CompileInfo;
import top.guyi.iot.ipojo.compile.lib.expand.CompileExpand;

import java.util.Set;

public class ServiceRegisterExpand implements CompileExpand {

    @Override
    public Set<CompileClass> execute(ClassPool pool, String path, CompileInfo compileInfo, Set<CompileClass> components) throws Exception {
        CtClass register = pool.get(ServiceRegister.class.getName());
        components.add(new CompileClass(register));

        CtMethod method = register.getDeclaredMethod("registerAll",new CtClass[0]);
        method.setModifiers(Modifier.PROTECTED);
        StringBuffer methodBody = new StringBuffer("{");
        components.stream()
                .filter(component -> component.getClasses().hasAnnotation(Service.class))
                .forEach(component -> {
                    try {
                        Service service = (Service) component.getClasses().getAnnotation(Service.class);
                        methodBody.append(String.format(
                                "$0.register(new %s(%s.class,%s.class));",
                                ServiceEntry.class.getName(),
                                service.value().getName(),
                                component.getClasses().getName()));
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                });
        methodBody.append("}");
        method.setBody(methodBody.toString());

        return components;
    }

}
