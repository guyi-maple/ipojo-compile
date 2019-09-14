package top.guyi.iot.ipojo.compile.lib.compile.entry;

import javassist.CtClass;
import lombok.Data;

@Data
public class CompileClass {

    private CtClass classes;
    private boolean write = true;

    public CompileClass(CtClass classes) {
        this.classes = classes;
    }

    public CompileClass(CtClass classes, boolean write) {
        this.classes = classes;
        this.write = write;
    }
}
