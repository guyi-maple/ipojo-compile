package top.guyi.iot.ipojo.compile.lib.utils;

import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.ArrayMemberValue;
import javassist.bytecode.annotation.MemberValue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author guyi
 * javassist注解工具
 */
public class AnnotationUtils {

    /**
     * 获取注解属性
     * @param classes classes
     * @return 注解属性
     */
    public static Optional<AnnotationsAttribute> getAnnotationAttribute(CtClass classes){
        classes.defrost();
        AnnotationsAttribute attribute = (AnnotationsAttribute) classes.getClassFile().getAttribute(AnnotationsAttribute.visibleTag);
        if (attribute == null){
            attribute = (AnnotationsAttribute) classes.getClassFile().getAttribute(AnnotationsAttribute.invisibleTag);
        }
        return Optional.ofNullable(attribute);
    }

    /**
     * 获取注解属性
     * @param method 方法
     * @return 注解属性
     */
    public static Optional<AnnotationsAttribute> getAnnotationAttribute(CtClass classes,CtMethod method){
        classes.defrost();
        AnnotationsAttribute attribute = (AnnotationsAttribute) method.getMethodInfo().getAttribute(AnnotationsAttribute.visibleTag);
        if (attribute == null){
            attribute = (AnnotationsAttribute) method.getMethodInfo().getAttribute(AnnotationsAttribute.invisibleTag);
        }
        return Optional.ofNullable(attribute);
    }

    /**
     * 获取注解属性
     * @param field 字段
     * @return 注解属性
     */
    public static Optional<AnnotationsAttribute> getAnnotationAttribute(CtClass classes,CtField field){
        classes.defrost();
        AnnotationsAttribute attribute = (AnnotationsAttribute) field.getFieldInfo().getAttribute(AnnotationsAttribute.visibleTag);
        if (attribute == null){
            attribute = (AnnotationsAttribute) field.getFieldInfo().getAttribute(AnnotationsAttribute.invisibleTag);
        }
        return Optional.ofNullable(attribute);
    }

    /**
     * 获取注解
     * @param attribute 注解属性
     * @param annotationClass 注解Class名称
     * @return 注解
     */
    public static Optional<Annotation> getAnnotation(AnnotationsAttribute attribute, String annotationClass){
        return Optional.ofNullable(attribute).map(a -> a.getAnnotation(annotationClass));
    }

    /**
     * 获取注解
     * @param classes classes
     * @param annotationClass 注解Class名称
     * @return 注解
     */
    public static Optional<Annotation> getAnnotation(CtClass classes, String annotationClass){
        return getAnnotationAttribute(classes).flatMap(attribute -> getAnnotation(attribute,annotationClass));
    }

    /**
     * 获取注解
     * @param method 方法
     * @param annotationClass 注解Class名称
     * @return 注解
     */
    public static Optional<Annotation> getAnnotation(CtClass classes, CtMethod method, String annotationClass){
        return getAnnotationAttribute(classes,method).flatMap(attribute -> getAnnotation(attribute,annotationClass));
    }

    /**
     * 获取注解
     * @param field 方法
     * @param annotationClass 注解Class名称
     * @return 注解
     */
    public static Optional<Annotation> getAnnotation(CtClass ctClass,CtField field, String annotationClass){
        return getAnnotationAttribute(ctClass,field).flatMap(attribute ->
                getAnnotation(attribute, annotationClass));
    }

    /**
     * 获取注解值
     * @param attribute 注解属性
     * @param annotationClass 注解Class名称
     * @param name 值名称
     * @return 注解值
     */
    public static Optional<MemberValue> getAnnotationValue(AnnotationsAttribute attribute, String annotationClass, String name){
        return getAnnotation(attribute, annotationClass).flatMap(a -> getAnnotationValue(a, name));
    }

    /**
     * 获取注解值
     * @param annotation 注解
     * @param name 值名称
     * @return 注解值
     */
    public static Optional<MemberValue> getAnnotationValue(Annotation annotation, String name){
        return Optional.ofNullable(annotation.getMemberValue(name));
    }

    /**
     * 获取注解值
     * @param classes classes
     * @param annotationClass 注解Class名称
     * @param name 值名称
     * @return 注解值
     */
    public static Optional<MemberValue> getAnnotationValue(CtClass classes, String annotationClass, String name){
        return getAnnotation(classes, annotationClass).flatMap(a -> getAnnotationValue(a, name));
    }

    /**
     * 获取注解值
     * @param method 方法
     * @param annotationClass 注解Class名称
     * @param name 值名称
     * @return 注解值
     */
    public static Optional<MemberValue> getAnnotationValue(CtClass classes,CtMethod method, String annotationClass, String name){
        return getAnnotation(classes,method,annotationClass).map(a -> a.getMemberValue(name));
    }

    /**
     * 获取注解值
     * @param field 字段
     * @param annotationClass 注解Class名称
     * @param name 值名称
     * @return 注解值
     */
    public static Optional<MemberValue> getAnnotationValue(CtClass classes, CtField field, String annotationClass, String name){
        return getAnnotation(classes,field,annotationClass).map(a -> a.getMemberValue(name));
    }

    /**
     * 获取注解值
     * @param annotation 注解
     * @param name 值名称
     * @return 注解值
     */
    public static List<MemberValue> getAnnotationValues(Annotation annotation,String name){
        MemberValue array = annotation.getMemberValue(name);
        if (array instanceof ArrayMemberValue){
            return Arrays.asList(((ArrayMemberValue) array).getValue());
        }else{
            return Collections.singletonList(array);
        }
    }

}
