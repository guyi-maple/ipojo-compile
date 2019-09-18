package top.guyi.iot.ipojo.compile.lib.utils;

import top.guyi.iot.ipojo.application.ApplicationContext;
import javassist.*;
import top.guyi.iot.ipojo.compile.lib.classes.exception.SetMethodNotFoundException;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class JavassistUtils {

    private static Pattern genericPattern = Pattern.compile("<L((.)+)>");

    public static void main(String[] args) {
        String str = "Ljava/util/Map<Ljava/lang/String;Ltop/guyi/test/ServiceInterface;>;";
        Matcher matcher = genericPattern.matcher(str);
        if (matcher.find()){
            System.out.println(matcher.group(1));
        }
    }

    public static List<CtClass> getGenerics(CtField field,ClassPool pool){
        String generic = field.getGenericSignature();
        if (!(generic.contains("<") && generic.contains(">"))){
            try {
                CtClass classes = pool.get(generic.substring(1));
                return Collections.singletonList(classes);
            } catch (NotFoundException e) {
                e.printStackTrace();
            }
            return Collections.emptyList();
        }
        Matcher matcher = genericPattern.matcher(field.getGenericSignature());
        if (matcher.find()){
            return Arrays.stream(matcher.group(1).split(";"))
                    .map(value -> value.replace("L","").replaceAll("/","."))
                    .map(value -> {
                        try {
                            return pool.get(value);
                        } catch (NotFoundException e) {
                            e.printStackTrace();
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

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
