package top.guyi.iot.ipojo.compile.lib.expand.compile.defaults.timer;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import lombok.AllArgsConstructor;
import lombok.Data;
import top.guyi.iot.ipojo.compile.lib.compile.entry.CompileClass;
import top.guyi.iot.ipojo.compile.lib.configuration.Compile;
import top.guyi.iot.ipojo.compile.lib.expand.compile.CompileExpand;

import java.util.Arrays;
import java.util.Set;

@Data
@AllArgsConstructor
class TimerEntry {

    private CtClass bean;
    private CtMethod method;

}

public class TimeCompileExpand implements CompileExpand {

    @Override
    public Set<CompileClass> execute(ClassPool pool, Compile compile, Set<CompileClass> components) throws Exception {
        components.forEach(bean -> Arrays.stream(bean.getClasses().getMethods()));

        return components;
    }

}
