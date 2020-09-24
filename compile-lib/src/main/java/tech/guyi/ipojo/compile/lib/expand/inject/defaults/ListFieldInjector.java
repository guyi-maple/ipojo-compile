package tech.guyi.ipojo.compile.lib.expand.inject.defaults;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import tech.guyi.ipojo.compile.lib.utils.StringUtils;
import tech.guyi.ipojo.compile.lib.classes.entry.FieldEntry;
import tech.guyi.ipojo.compile.lib.expand.inject.FieldInjector;
import tech.guyi.ipojo.compile.lib.utils.JavassistUtils;

import java.util.List;

public class ListFieldInjector implements FieldInjector {

    @Override
    public boolean check(FieldEntry field, ClassPool pool) {
        try {
            return field.getField().getType().getName().equals(List.class.getName());
        } catch (NotFoundException e) {
            return false;
        }
    }

    @Override
    public String injectCode(FieldEntry field, CtMethod setMethod, ClassPool pool) {
        try{
            List<CtClass> types = JavassistUtils.getGenerics(field.getField(),pool);
            if (types.size() == 1){
                if (StringUtils.isEmpty(field.getName())){
                    return String.format(
                            "$0.%s((%s)$1.getList(%s.class));",
                            setMethod.getName(),
                            setMethod.getParameterTypes()[0].getName(),
                            types.get(0).getName()
                    );
                }else{
                    return String.format(
                            "$0.%s((%s)$1.getList(%s.class,\"%s\"));",
                            setMethod.getName(),
                            setMethod.getParameterTypes()[0].getName(),
                            types.get(0).getName(),
                            field.getName()
                    );
                }
            }
        }catch (NotFoundException e){
            e.printStackTrace();
        }
        return null;
    }

}
