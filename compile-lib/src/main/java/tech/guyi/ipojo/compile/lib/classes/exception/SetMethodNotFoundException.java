package tech.guyi.ipojo.compile.lib.classes.exception;

import javassist.CtClass;
import javassist.CtField;

public class SetMethodNotFoundException extends RuntimeException {

    public SetMethodNotFoundException(CtClass ctClass, CtField field){
        super(String.format("找不到字段[%s][%s]的Set方法",ctClass.getName(),field.getName()));
    }

}
