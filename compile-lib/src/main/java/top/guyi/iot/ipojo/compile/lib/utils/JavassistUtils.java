package top.guyi.iot.ipojo.compile.lib.utils;

import lombok.SneakyThrows;
import javassist.*;
import top.guyi.iot.ipojo.compile.lib.classes.exception.SetMethodNotFoundException;
import top.guyi.iot.ipojo.compile.lib.cons.ClassNames;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class JavassistUtils {

    private static final Pattern genericPattern = Pattern.compile("<L((.)+)>");

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
                    .map(value -> {
                        if (value.startsWith("L")){
                            value = value.substring(1);
                        }
                        return value.replaceAll("/",".");
                    })
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

    @SneakyThrows
    public static Optional<CtConstructor> getDeclaredConstructor(CtClass classes,CtClass[] args){
        return Optional.ofNullable(classes.getDeclaredConstructor(args));
    }


    @SneakyThrows
    public static void insertAfter(CtMethod method, String body){
        method.insertAfter(body);
    }

    @SneakyThrows
    public static void insertAfter(CtConstructor constructor, String body){
        constructor.insertAfter(body);
    }

    public static Optional<CtMethod> getInjectMethodNullable(ClassPool pool, CtClass classes) {
        try {
            return Optional.ofNullable(classes.getDeclaredMethod("inject",new CtClass[]{pool.get(ClassNames.ApplicationContext)}));
        } catch (NotFoundException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public static CtMethod getInjectMethod(ClassPool pool, CtClass classes) throws NotFoundException {
        return classes.getDeclaredMethod("inject",new CtClass[]{pool.get(ClassNames.ApplicationContext)});
    }

    private static Map<String,CtField> listField(CtClass stopClass,CtClass classes,Map<String,CtField> fields){
        List<CtField> fieldList = new LinkedList<>();
        fieldList.addAll(Arrays.asList(classes.getDeclaredFields()));
        fieldList.addAll(Arrays.asList(classes.getFields()));
        fieldList
                .stream()
                .filter(field -> !fields.containsKey(field.getName()))
                .forEach(field -> fields.put(field.getName(),field));

        try {
            CtClass superClass = classes.getSuperclass();
            if (!superClass.getName().equals(stopClass.getName())){
                return listField(stopClass,superClass,fields);
            }
        } catch (NotFoundException e) {
            e.printStackTrace();
        }

        return fields;
    }

    public static <T> List<T> getFields(CtClass stopClass, CtClass classes, Function<CtField,T> converter){
        return listField(stopClass,classes,new HashMap<>()).values()
                .stream()
                .map(converter)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public static <T> List<T> getFields(CtClass stopClass, CtClass classes, BiFunction<CtClass,CtField,T> converter){
        return listField(stopClass,classes,new HashMap<>()).values()
                .stream()
                .map(field -> converter.apply(classes,field))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public static Optional<CtClass> getFieldType(CtField field){
        try {
            return Optional.ofNullable(field.getType());
        } catch (NotFoundException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public static boolean equalsType(CtClass classes,CtClass type){
        try {
            return classes.subtypeOf(type);
        } catch (NotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean equalsType(CtField field,CtClass type){
        try {
            return equalsType(field.getType(),type);
        } catch (NotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    @SneakyThrows
    public static CtClass get(ClassPool pool,String className){
        return pool.get(className);
    }

}
