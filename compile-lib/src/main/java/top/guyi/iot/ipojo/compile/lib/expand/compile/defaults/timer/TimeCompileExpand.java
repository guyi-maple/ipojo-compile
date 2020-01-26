package top.guyi.iot.ipojo.compile.lib.expand.compile.defaults.timer;

import javassist.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import top.guyi.iot.ipojo.application.ApplicationContext;
import top.guyi.iot.ipojo.application.osgi.timer.AbstractTimerManager;
import top.guyi.iot.ipojo.application.osgi.timer.TimerRunnable;
import top.guyi.iot.ipojo.application.osgi.timer.annotation.Timer;
import top.guyi.iot.ipojo.application.osgi.timer.defaults.MethodTimerRunnable;
import top.guyi.iot.ipojo.application.osgi.timer.enums.TimeType;
import top.guyi.iot.ipojo.application.utils.StringUtils;
import top.guyi.iot.ipojo.compile.lib.compile.entry.CompileClass;
import top.guyi.iot.ipojo.compile.lib.configuration.Compile;
import top.guyi.iot.ipojo.compile.lib.expand.compile.CompileExpand;

import java.util.*;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
class TimerEntry {

    private CtClass bean;
    private Timer timer;
    private CtMethod method;

}

public class TimeCompileExpand implements CompileExpand {

    @Override
    public Set<CompileClass> execute(ClassPool pool, Compile compile, Set<CompileClass> components) throws Exception {
        Set<TimerEntry> timers = components
                .stream()
                .map(bean ->
                        Arrays.stream(bean.getClasses().getMethods())
                                .map(method -> {
                                    try {
                                        Timer timer = (Timer) method.getAnnotation(Timer.class);
                                        if (timer != null){
                                            return new TimerEntry(bean.getClasses(),timer,method);
                                        }
                                    } catch (ClassNotFoundException e) {
                                        e.printStackTrace();
                                    }
                                    return null;
                                })
                                .collect(Collectors.toSet())
                )
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        CtClass timerRegister = pool.makeClass(String.format("%s.DefaultTimeManager",compile.getPackageName()));
        timerRegister.setSuperclass(pool.get(AbstractTimerManager.class.getName()));
        CtMethod registerAll = new CtMethod(CtClass.voidType,"registerAll",new CtClass[0],timerRegister);
        StringBuilder registerMethodBody = new StringBuilder("{\n");
        for (TimerEntry timer : timers) {
            CtClass run = this.createTimerRunnable(compile,pool,timer);
            components.add(new CompileClass(run,false,false,false));
            run.writeFile(compile.getProject().getWork());
            registerMethodBody.append(String.format(
                    "$0.register((%s)new %s());\n",
                    TimerRunnable.class.getName(),
                    run.getName()
            ));
        }
        registerMethodBody.append("}");
        registerAll.setBody(registerMethodBody.toString());
        timerRegister.addMethod(registerAll);
        components.add(new CompileClass(timerRegister,true));

        return components;
    }

    private CtClass createTimerRunnable(Compile compile,ClassPool pool,TimerEntry entry) throws NotFoundException, CannotCompileException {
        String classesName = String.format("%s.TimerRunnable%s",compile.getPackageName(),UUID.randomUUID().toString().replaceAll("-",""));
        CtClass classes = pool.makeClass(classesName);
        classes.setSuperclass(pool.get(MethodTimerRunnable.class.getName()));

        CtConstructor constructor = new CtConstructor(new CtClass[0],classes);
        constructor.setBody(String.format(
                "{super(\"%s\",%s,%s.%s);}",
                StringUtils.isEmpty(entry.getTimer().name()) ? UUID.randomUUID().toString() : entry.getTimer().name(),
                entry.getTimer().delay(),
                TimeType.class.getName(),
                entry.getTimer().type().toString()
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

        CtMethod runMethod = new CtMethod(CtClass.voidType,"run",new CtClass[]{pool.get(ApplicationContext.class.getName())},classes);
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
