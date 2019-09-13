package top.guyi.iot.ipojo.compile.lib.classes;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class ClassCompiler {

    private ClassScanner scanner = new ClassScanner();
    private ClassEditor editor = new ClassEditor();

    public Set<CtClass> compile(ClassPool pool,String path) {
        return this.scanner.getComponent(pool,path)
                .stream()
                .map(component -> {
                    try {
                        editor.addInjectMethod(pool,component);
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
