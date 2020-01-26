package top.guyi.iot.ipojo.compile.lib.compile.entry;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import lombok.Data;
import top.guyi.iot.ipojo.application.component.condition.ConditionOnMissBean;

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
        boolean register = true;
        try {
            ConditionOnMissBean conditionOnMissBean = (ConditionOnMissBean) this.classes.getAnnotation(ConditionOnMissBean.class);
            if (conditionOnMissBean != null){
                CtClass condition = pool.get(conditionOnMissBean.value().getName());
                register = components
                        .stream()
                        .filter(component -> !component.equals(this))
                        .noneMatch(component -> {
                            try {
                                return component.getClasses().subtypeOf(condition);
                            } catch (NotFoundException e) {
                                e.printStackTrace();
                            }
                            return false;
                        });
            }
        } catch (ClassNotFoundException | NotFoundException e) {
            e.printStackTrace();
        }
        return register;
    }

}
