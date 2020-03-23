package top.guyi.iot.ipojo.compile.lib.expand.compile.defaults.log;

import top.guyi.iot.ipojo.application.osgi.log.Log;
import top.guyi.iot.ipojo.application.osgi.log.AbstractLoggerRepository;
import top.guyi.iot.ipojo.compile.lib.expand.compile.defaults.log.entry.ComponentLoggerEntry;
import top.guyi.iot.ipojo.compile.lib.expand.compile.defaults.log.entry.LoggerEntry;
import top.guyi.iot.ipojo.compile.lib.compile.entry.CompileClass;
import top.guyi.iot.ipojo.compile.lib.configuration.Compile;
import top.guyi.iot.ipojo.compile.lib.enums.CompileType;
import top.guyi.iot.ipojo.compile.lib.expand.compile.CompileExpand;
import top.guyi.iot.ipojo.compile.lib.utils.JavassistUtils;
import javassist.*;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 日志拓展
 */
public class LoggerExpand implements CompileExpand {

    @Override
    public boolean check(Compile compile) {
        return true;
    }

    @Override
    public Set<CompileClass> execute(ClassPool pool, Compile compile, Set<CompileClass> components) throws Exception {
        List<ComponentLoggerEntry> entries = components
                .stream()
                .map(component -> new ComponentLoggerEntry(
                        component,
                        Arrays.stream(component.getClasses().getDeclaredFields())
                                .map(field -> {
                                    try {
                                        Log log = (Log) field.getAnnotation(Log.class);
                                        if (log != null){
                                            return new LoggerEntry(field,log.value());
                                        }
                                    } catch (ClassNotFoundException e) {
                                        e.printStackTrace();
                                    }
                                    return null;
                                })
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList())
                        )
                )
                .filter(entry -> entry.getLoggerEntry().size() > 0)
                .filter(entry -> compile.getType() != CompileType.BUNDLE
                        || entry.getComponent().getClasses().getPackageName().startsWith(compile.getPackageName()))
                .collect(Collectors.toList());
        if (!entries.isEmpty()){
            CtClass repository = pool.makeClass(String.format("%s.DefaultAutoLoggerRepository",compile.getPackageName()));
            repository.setSuperclass(pool.get(AbstractLoggerRepository.class.getName()));

            if (compile.getType() == CompileType.BUNDLE){
                components.add(new CompileClass(repository,true,true,false));
            }

            entries.forEach(entry -> {
                try {
                    StringBuffer injectAfter = new StringBuffer();
                    CtMethod injectMethod = JavassistUtils.getInjectMethod(pool,entry.getComponent().getClasses());
                    entry.getLoggerEntry().forEach(logger -> {
                        CtMethod setMethod = JavassistUtils.getSetMethod(
                                entry.getComponent().getClasses(),logger.getField());
                        if (compile.getType() == CompileType.BUNDLE){
                            injectAfter.append(String.format(
                                    "$0.%s(((%s)$1.get(%s.class,true)).get(\"%s\"));",
                                    setMethod.getName(),
                                    repository.getName(),
                                    repository.getName(),
                                    logger.getLoggerName()
                            ));
                        }else {
                            injectAfter.append(String.format(
                                    "$0.%s(((%s)$1.get(%s.class)).get(\"%s\"));",
                                    setMethod.getName(),
                                    AbstractLoggerRepository.class.getName(),
                                    AbstractLoggerRepository.class.getName(),
                                    logger.getLoggerName()
                            ));
                        }
                    });
                    injectMethod.insertAfter(injectAfter.toString());
                } catch (NotFoundException | CannotCompileException e) {
                    e.printStackTrace();
                }
            });
        }

        return components;
    }

}
