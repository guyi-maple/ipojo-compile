package top.guyi.iot.ipojo.compile.lib.utils;

import top.guyi.iot.ipojo.application.ApplicationContext;
import javassist.*;
import top.guyi.iot.ipojo.compile.lib.classes.exception.SetMethodNotFoundException;

public class JavassistUtils {

    public static CtMethod getSetMethod(CtClass classes, CtField field){
        String setMethodName = "set" + field.getName().substring(0,1).toUpperCase() + field.getName().substring(1);
        CtMethod setMethod;
        try {
            setMethod = classes.getDeclaredMethod(setMethodName,new CtClass[]{field.getType()});
        } catch (NotFoundException e) {
            try {
                setMethod = CtNewMethod.setter(setMethodName,field);
                classes.addMethod(setMethod);
            } catch (CannotCompileException ex) {
                setMethod = null;
                for (CtMethod method : classes.getMethods()) {
                    try {
                        if (method.getName().equals(setMethodName)
                                && method.getParameterTypes().length == 1
                                && method.getParameterTypes()[0].getName().equals(field.getType().getName())){
                            setMethod = method;
                            break;
                        }
                    } catch (NotFoundException exc) {
                        exc.printStackTrace();
                    }
                }
                if (setMethod == null){
                    throw new SetMethodNotFoundException(classes,field);
                }
            }
        }
        return setMethod;
    }

    public static CtMethod getInjectMethod(ClassPool pool,CtClass classes) throws NotFoundException {
        return classes.getDeclaredMethod("inject",new CtClass[]{pool.get(ApplicationContext.class.getName())});
    }

}
