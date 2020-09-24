package top.guyi.ipojo.compile.lib.compile.entry;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.bytecode.annotation.ClassMemberValue;
import lombok.Data;
import top.guyi.ipojo.compile.lib.cons.AnnotationNames;
import top.guyi.ipojo.compile.lib.utils.AnnotationUtils;
import top.guyi.ipojo.compile.lib.utils.JavassistUtils;

import java.util.Set;

@Data
public class CompileClass {

    private String name;
    private CtClass classes;
    private boolean write = true;
    private boolean component = true;
    private boolean proxy = true;
    private int order = 999;

    public CompileClass(CtClass classes) {
        this.classes = classes;
        this.name = classes.getSimpleName();
    }

    public CompileClass(CtClass classes, boolean write) {
        this.classes = classes;
        this.write = write;
        this.name = classes.getSimpleName();
    }

    public CompileClass(boolean component,CtClass classes) {
        this.classes = classes;
        this.component = component;
        this.name = classes.getSimpleName();
    }

    public CompileClass(CtClass classes, boolean write, boolean component, boolean proxy) {
        this.classes = classes;
        this.write = write;
        this.component = component;
        this.proxy = proxy;
        this.name = classes.getSimpleName();
    }

    public CompileClass(String name, CtClass classes, boolean write, boolean component, boolean proxy, int order) {
        this.name = name;
        this.classes = classes;
        this.write = write;
        this.component = component;
        this.proxy = proxy;
        this.order = order;
    }

    public boolean isRegister(ClassPool pool,Set<CompileClass> components){
        return AnnotationUtils.getAnnotation(this.classes, AnnotationNames.ConditionOnMissBean)
                .flatMap(annotation -> AnnotationUtils.getAnnotationValue(annotation,"value"))
                .map(value -> (ClassMemberValue) value)
                .map(ClassMemberValue::getValue)
                .map(value -> JavassistUtils.get(pool,value))
                .map(condition -> components.stream()
                        .filter(component -> !component.equals(this))
                        .noneMatch(component -> JavassistUtils.equalsType(component.getClasses(),condition)))
                .orElse(true);
    }

}
