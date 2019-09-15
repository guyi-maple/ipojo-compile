package top.guyi.iot.ipojo.compile.lib.classes;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.NotFoundException;
import top.guyi.iot.ipojo.compile.lib.compile.entry.CompileClass;
import top.guyi.iot.ipojo.compile.lib.configuration.Compile;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class ClassCompiler {

    private ClassScanner scanner = new ClassScanner();
    private ClassEditor editor = new ClassEditor();

    public Set<CompileClass> compile(ClassPool pool, Compile compile) {
        return this.scanner.getComponent(pool,compile.getProject().getWork())
                .stream()
                .map(component -> {
                    try {
                        editor.addInjectMethod(pool,component.getClasses());
                        return component;
                    } catch (CannotCompileException | NotFoundException e) {
                        e.printStackTrace();
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

}
