package top.guyi.ipojo.compile.lib.expand.compile.defaults.timer;

import javassist.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import top.guyi.ipojo.compile.lib.compile.entry.CompileClass;
import top.guyi.ipojo.compile.lib.configuration.Compile;
import top.guyi.ipojo.compile.lib.cons.AnnotationNames;
import top.guyi.ipojo.compile.lib.cons.ClassNames;
import top.guyi.ipojo.compile.lib.expand.compile.CompileExpand;
import top.guyi.ipojo.compile.lib.utils.AnnotationUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
class TimerEntry {

    private CtClass bean;
    private TimeAnnotationEntry timer;
    private CtMethod method;

}

/**
 * @author guyi
 * 定时器拓展
 */
public class TimeCompileExpand implements CompileExpand {

    @Override
    public Set<CompileClass> execute(ClassPool pool, Compile compile, Set<CompileClass> components) throws Exception {
        Set<TimerEntry> timers = components
                .stream()
                .map(bean ->
                        Arrays.stream(bean.getClasses().getMethods())
                                .map(method -> AnnotationUtils
                                        .getAnnotation(bean.getClasses(),method, AnnotationNames.Timer)
                                        .map(annotation -> new TimerEntry(bean.getClasses(),new TimeAnnotationEntry(annotation),method))
                                        .orElse(null))
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet())
                )
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        if (!timers.isEmpty()) {
            CtClass timerRegister = pool.makeClass(String.format("%s.timer.DefaultTimeManager",compile.getPackageName()));
            timerRegister.setSuperclass(pool.get(ClassNames.AbstractTimerManager));
            compile.addUseComponent(timerRegister);
            CtMethod registerAll = new CtMethod(CtClass.voidType,"registerAll",new CtClass[0],timerRegister);
            StringBuilder registerMethodBody = new StringBuilder("{\n");
            for (TimerEntry timer : timers) {
                CtClass run = this.createTimerRunnable(compile,pool,timer);
                components.add(new CompileClass(run,false,false,false));
                run.writeFile(compile.getProject().getWork());
                compile.formatJavaVersion(run.getURL());
                registerMethodBody.append(String.format(
                        "$0.register((%s)new %s());\n",
                        ClassNames.TimerRunnable,
                        run.getName()
                ));
            }
            registerMethodBody.append("}");
            registerAll.setBody(registerMethodBody.toString());
            timerRegister.addMethod(registerAll);
            components.add(new CompileClass(timerRegister,true));
        }

        return components;
    }

    private CtClass createTimerRunnable(Compile compile,ClassPool pool,TimerEntry entry) throws NotFoundException, CannotCompileException {
        String classesName = String.format("%s.timer.runnable.TimerRunnable%s",compile.getPackageName(),UUID.randomUUID().toString().replaceAll("-",""));
        CtClass classes = pool.makeClass(classesName);
        classes.setSuperclass(pool.get(ClassNames.AbstractMethodTimerRunnable));

        CtConstructor constructor = new CtConstructor(new CtClass[0],classes);
        constructor.setBody(String.format(
                "{super(\"%s\",%s,%s,%s.%s,%s.%s);}",
                ("".equals(entry.getTimer().getName()))? UUID.randomUUID().toString() : entry.getTimer().getName(),
                entry.getTimer().getInitDelay(),
                entry.getTimer().getDelay(),
                ClassNames.TimeType,
                entry.getTimer().getType(),
                TimeUnit.class.getName(),
                entry.getTimer().getUnit()
        ));
        classes.addConstructor(constructor);


        StringBuilder argsBody = new StringBuilder();
        CtClass[] args = entry.getMethod().getParameterTypes();
        if (args != null && args.length > 0){
            for (CtClass type : args) {
                argsBody.append(String.format(
                        "((%s)$1.get(%s.class,true)),",
                        type.getName(),type.getName())
                );
            }
        }else{
            argsBody.append(" ");
        }

        CtMethod runMethod = new CtMethod(CtClass.voidType,"run",new CtClass[]{pool.get(ClassNames.ApplicationContext)},classes);
        runMethod.setBody(String.format(
                "{((%s)$1.get(%s.class,true)).%s(%s);}",
                entry.getBean().getName(),
                entry.getBean().getName(),
                entry.getMethod().getName(),
                argsBody.substring(0,argsBody.length() - 1)
        ));

        classes.addMethod(runMethod);

        return classes;
    }

}
