package tech.guyi.ipojo.compile.lib.classes;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.NotFoundException;
import tech.guyi.ipojo.compile.lib.compile.entry.CompileClass;
import tech.guyi.ipojo.compile.lib.configuration.Compile;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class ClassCompiler {

    private ClassScanner scanner = new ClassScanner();
    private ClassEditor editor = new ClassEditor();

    public Set<CompileClass> compile(ClassPool pool, Compile compile) throws IOException, NotFoundException {
        return this.scanner.getComponent(pool,compile)
                .stream()
                .map(component -> {
                    try {
                        editor.addInjectMethod(pool,component.getClasses(),compile);
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
