package top.guyi.iot.ipojo.compile.lib.expand.compile.defaults.stream.entry;

import javassist.CtClass;
import javassist.CtField;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FieldEntry {

    private boolean sync;
    private CtClass classes;
    private CtField field;

}
