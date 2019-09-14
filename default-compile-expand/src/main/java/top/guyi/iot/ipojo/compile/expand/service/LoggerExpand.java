package top.guyi.iot.ipojo.compile.expand.service;

import top.guyi.iot.ipojo.application.osgi.log.Log;
import top.guyi.iot.ipojo.application.osgi.log.AbstractLoggerRepository;
import top.guyi.iot.ipojo.compile.lib.compile.entry.CompileClass;
import top.guyi.iot.ipojo.compile.lib.configuration.CompileInfo;
import top.guyi.iot.ipojo.compile.lib.enums.CompileType;
import top.guyi.iot.ipojo.compile.lib.expand.CompileExpand;
import top.guyi.iot.ipojo.compile.lib.utils.JavassistUtils;
import javassist.*;

import java.util.Arrays;
import java.util.Set;

public class LoggerExpand implements CompileExpand {

    @Override
    public Set<CompileClass> execute(ClassPool pool, String path, CompileInfo compileInfo, Set<CompileClass> components) throws Exception {
        if (compileInfo.getType() == CompileType.COMPONENT){
            return components;
        }

        CtClass repository = pool.get(AbstractLoggerRepository.class.getName());
        components.add(new CompileClass(repository));

        components.forEach(component -> {
            StringBuilder sb = new StringBuilder();
            Arrays.stream(component.getClasses().getDeclaredFields())
                    .filter(field -> field.hasAnnotation(Log.class))
                    .forEach(field -> {
                        try {
                            Log log = (Log) field.getAnnotation(Log.class);
                            CtMethod setMethod = JavassistUtils.getSetMethod(component.getClasses(),field);
                            sb.append(String.format(
                                    "$0.%s(%s.get(\"%s\"));\n",
                                    setMethod.getName(),
                                    AbstractLoggerRepository.class.getName(),
                                    log.value())
                            );
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                    });
            try {
                CtMethod inject = JavassistUtils.getInjectMethod(pool,component.getClasses());
                inject.insertAfter(sb.toString());
            } catch (CannotCompileException e) {
                e.printStackTrace();
            } catch (NotFoundException e) {
            }
        });

        return components;
    }

}
