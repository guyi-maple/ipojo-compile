package top.guyi.iot.ipojo.compile.lib.expand.inject.defaults;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import top.guyi.iot.ipojo.application.component.ForType;
import top.guyi.iot.ipojo.application.utils.StringUtils;
import top.guyi.iot.ipojo.compile.lib.classes.entry.FieldEntry;
import top.guyi.iot.ipojo.compile.lib.expand.inject.FieldInjector;
import top.guyi.iot.ipojo.compile.lib.utils.JavassistUtils;

import java.util.List;
import java.util.Map;

public class MapFieldInjector implements FieldInjector {

    @Override
    public boolean check(FieldEntry field, ClassPool pool) {
        try {
            return field.getField().getType().getName().equals(Map.class.getName());
        } catch (NotFoundException e) {
            return false;
        }
    }

    @Override
    public String injectCode(FieldEntry field, CtMethod setMethod, ClassPool pool) {
        List<CtClass> types = JavassistUtils.getGenerics(field.getField(),pool);
        try {
            if (types.size() == 2
                    && types.get(0).getName().equals(String.class.getName())
                    && types.get(1).subtypeOf(pool.get(ForType.class.getName()))){

                if (StringUtils.isEmpty(field.getName())){
                    return String.format(
                            "$0.%s((%s)$1.getMap(%s.class));",
                            setMethod.getName(),
                            setMethod.getParameterTypes()[0].getName(),
                            types.get(1).getName()
                    );
                }else{
                    return String.format(
                            "$0.%s((%s)$1.getMap(%s.class,\"%s\"));",
                            setMethod.getName(),
                            setMethod.getParameterTypes()[0].getName(),
                            types.get(1).getName(),
                            field.getName()
                    );
                }
            }
        } catch (NotFoundException e) {
            return null;
        }
        return null;
    }

}