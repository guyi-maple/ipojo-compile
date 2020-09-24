package top.guyi.ipojo.compile.lib.expand.inject;

import javassist.ClassPool;
import javassist.CtMethod;
import top.guyi.ipojo.compile.lib.classes.entry.FieldEntry;

public interface FieldInjector {

    default int order(){
        return 999;
    }

    boolean check(FieldEntry field, ClassPool pool);

    String injectCode(FieldEntry field, CtMethod setMethod, ClassPool pool);

}
