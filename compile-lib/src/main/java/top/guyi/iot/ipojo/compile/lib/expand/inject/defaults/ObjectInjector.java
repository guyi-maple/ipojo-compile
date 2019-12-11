package top.guyi.iot.ipojo.compile.lib.expand.inject.defaults;

import javassist.ClassPool;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import top.guyi.iot.ipojo.application.utils.StringUtils;
import top.guyi.iot.ipojo.compile.lib.classes.entry.FieldEntry;
import top.guyi.iot.ipojo.compile.lib.expand.inject.FieldInjector;

public class ObjectInjector implements FieldInjector {

    @Override
    public boolean check(FieldEntry field, ClassPool pool) {
        return true;
    }

    @Override
    public String injectCode(FieldEntry field, CtMethod setMethod, ClassPool pool) {
        try{
            if ((!field.isEquals())
                    || (field.getField().getType().isInterface())
                    || (Modifier.isAbstract(field.getField().getType().getModifiers()))){
                return String.format(
                        "$0.%s((%s) $1.get(%s.class));",
                        setMethod.getName(),
                        field.getField().getType().getName(),
                        field.getField().getType().getName()
                );
            }

            if (StringUtils.isEmpty(field.getName())){
                return String.format(
                        "$0.%s((%s) $1.get(%s.class,true));",
                        setMethod.getName(),
                        field.getField().getType().getName(),
                        field.getField().getType().getName()
                );
            }

            return String.format(
                    "$0.%s((%s) $1.get(%s.class,\"%s\"));",
                    setMethod.getName(),
                    field.getField().getType().getName(),
                    field.getField().getType().getName(),
                    field.getName()
            );
        }catch (NotFoundException e){
            e.printStackTrace();
            return null;
        }
    }

}
