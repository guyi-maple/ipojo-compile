package top.guyi.ipojo.compile.lib.expand.compile.defaults.log;

import javassist.bytecode.annotation.StringMemberValue;
import top.guyi.ipojo.compile.lib.cons.AnnotationNames;
import top.guyi.ipojo.compile.lib.cons.ClassNames;
import top.guyi.ipojo.compile.lib.expand.compile.defaults.log.entry.ComponentLoggerEntry;
import top.guyi.ipojo.compile.lib.expand.compile.defaults.log.entry.LoggerEntry;
import top.guyi.ipojo.compile.lib.compile.entry.CompileClass;
import top.guyi.ipojo.compile.lib.configuration.Compile;
import top.guyi.ipojo.compile.lib.enums.CompileType;
import top.guyi.ipojo.compile.lib.expand.compile.CompileExpand;
import top.guyi.ipojo.compile.lib.utils.AnnotationUtils;
import top.guyi.ipojo.compile.lib.utils.JavassistUtils;
import javassist.*;

import java.util.*;
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
                                .filter(field -> AnnotationUtils.getAnnotation(component.getClasses(),field, AnnotationNames.Log).isPresent())
                                .map(field -> AnnotationUtils
                                        .getAnnotationValue(component.getClasses(),field,AnnotationNames.Log,"value")
                                        .map(value -> new LoggerEntry(field,((StringMemberValue) value).getValue()))
                                        .orElse(new LoggerEntry(field, "default")))
                                .collect(Collectors.toList())
                        )
                )
                .filter(entry -> entry.getLoggerEntry().size() > 0)
                .filter(entry -> compile.getType() != CompileType.BUNDLE
                        || entry.getComponent().getClasses().getPackageName().startsWith(compile.getPackageName()))
                .collect(Collectors.toList());

        CtClass repository = pool.makeClass(String.format("%s.DefaultAutoLoggerRepository",compile.getPackageName()));
        repository.setSuperclass(pool.get(ClassNames.AbstractLoggerRepository));
        compile.addUseComponent(repository);
        if (compile.getType() == CompileType.BUNDLE){
            components.add(new CompileClass(repository,true,true,false));
        }

        entries.forEach(entry -> {
            try {
                StringBuffer injectAfter = new StringBuffer();
                CtMethod injectMethod = JavassistUtils.getInjectMethodNullable(pool,entry.getComponent().getClasses())
                        .orElseThrow(RuntimeException::new);
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
                                ClassNames.AbstractLoggerRepository,
                                ClassNames.AbstractLoggerRepository,
                                logger.getLoggerName()
                        ));
                    }
                });
                injectMethod.insertAfter(injectAfter.toString());
            } catch (CannotCompileException e) {
                e.printStackTrace();
            }
        });

        return components;
    }

}
