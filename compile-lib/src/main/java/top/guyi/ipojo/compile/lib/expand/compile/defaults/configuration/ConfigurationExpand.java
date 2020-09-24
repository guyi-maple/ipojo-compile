package top.guyi.ipojo.compile.lib.expand.compile.defaults.configuration;

import javassist.*;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.BooleanMemberValue;
import javassist.bytecode.annotation.StringMemberValue;
import top.guyi.ipojo.compile.lib.utils.StringUtils;
import top.guyi.ipojo.compile.lib.compile.entry.CompileClass;
import top.guyi.ipojo.compile.lib.configuration.Compile;
import top.guyi.ipojo.compile.lib.cons.AnnotationNames;
import top.guyi.ipojo.compile.lib.expand.compile.CompileExpand;
import top.guyi.ipojo.compile.lib.expand.compile.defaults.configuration.entry.ConfigurationField;
import top.guyi.ipojo.compile.lib.expand.compile.defaults.configuration.entry.ConfigurationKeyEntry;
import top.guyi.ipojo.compile.lib.utils.AnnotationUtils;
import top.guyi.ipojo.compile.lib.utils.JavassistUtils;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/**
 * @author guyi
 * 配置拓展
 */
public class ConfigurationExpand implements CompileExpand {

    @Override
    public Set<CompileClass> execute(ClassPool pool, Compile compile, Set<CompileClass> components) throws Exception {
        CtClass string = pool.get(String.class.getName());
        CtClass object = pool.get(Object.class.getName());
        components.stream()
                .map(component -> JavassistUtils.getFields(
                        object,
                        component.getClasses(),
                        (classes, field) -> Optional.ofNullable(field)
                                .map(f -> {
                                    boolean b = JavassistUtils.equalsType(f,string);
                                    Optional<Annotation> annotation = AnnotationUtils.getAnnotation(classes,f, AnnotationNames.ConfigurationKey);
                                    return f;
                                })
                                .filter(f -> JavassistUtils.equalsType(f,string))
                                .flatMap(f -> AnnotationUtils.getAnnotation(classes,f, AnnotationNames.ConfigurationKey))
                                .map(annotation -> new ConfigurationField(
                                        component,
                                        field,
                                        new ConfigurationKeyEntry(
                                                AnnotationUtils.getAnnotationValue(annotation,"key")
                                                        .map(value -> ((StringMemberValue)value).getValue())
                                                        .orElse(""),
                                                AnnotationUtils.getAnnotationValue(annotation,"remark")
                                                        .map(value -> ((StringMemberValue)value).getValue())
                                                        .orElse(""),
                                                AnnotationUtils.getAnnotationValue(annotation,"file")
                                                        .map(value -> ((BooleanMemberValue)value).getValue())
                                                        .orElse(false),
                                                component.getClasses().getSimpleName()
                                        )
                                ))
                        )
                )
                .flatMap(Collection::stream)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .peek(field -> compile.getConfigurationKeys().add(field.getKey()))
                .forEach(field -> {
                    String name = Optional.of(field.getKey().getKey())
                            .filter(n -> !StringUtils.isEmpty(n))
                            .orElse(field.getField().getName());

                    if (field.getKey().isFile()){
                        JavassistUtils.getInjectMethodNullable(pool,field.getComponent().getClasses())
                                .ifPresent(method -> {
                                    JavassistUtils.insertAfter(method,String.format(
                                            "$0.%s((%s)$1.getConfigurationFile(\"%s\",%s.class,$0.%s));",
                                            JavassistUtils.getSetMethod(field.getComponent().getClasses(),field.getField()).getName(),
                                            JavassistUtils.getFieldType(field.getField())
                                                    .map(CtClass::getName)
                                                    .orElseThrow(RuntimeException::new),
                                            field.getKey().getKey(),
                                            JavassistUtils.getFieldType(field.getField())
                                                    .map(CtClass::getName)
                                                    .orElseThrow(RuntimeException::new),
                                            field.getField().getName()
                                    ));
                                    field.getComponent().setWrite(true);
                                });
                    }

                    Optional.ofNullable(compile.getConfiguration().get(name))
                            .ifPresent(value ->
                                    JavassistUtils.getDeclaredConstructor(field.getComponent().getClasses(), new CtClass[0])
                                    .ifPresent(constructor -> {
                                        JavassistUtils.insertAfter(
                                                constructor,
                                                String.format(
                                                        "$0.%s = \"%s\";\n",
                                                        field.getField().getName(),
                                                        value
                                                )
                                        );
                                        field.getComponent().setWrite(true);
                                    })
                            );
                });

        return components;
    }

}
