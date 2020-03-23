package top.guyi.iot.ipojo.compile.lib.expand.compile.defaults.configuration;

import javassist.*;
import top.guyi.iot.ipojo.application.osgi.configuration.annotation.ConfigurationKey;
import top.guyi.iot.ipojo.application.utils.StringUtils;
import top.guyi.iot.ipojo.compile.lib.compile.entry.CompileClass;
import top.guyi.iot.ipojo.compile.lib.configuration.Compile;
import top.guyi.iot.ipojo.compile.lib.expand.compile.CompileExpand;
import top.guyi.iot.ipojo.compile.lib.expand.compile.defaults.configuration.entry.ConfigurationField;
import top.guyi.iot.ipojo.compile.lib.utils.JavassistUtils;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
                        object,component.getClasses(), field -> {
                            try {
                                if (!field.getType().subtypeOf(string)){
                                    return null;
                                }

                                ConfigurationKey configurationKey = (ConfigurationKey) field.getAnnotation(ConfigurationKey.class);
                                if (configurationKey == null){
                                    return null;
                                }
                                return new ConfigurationField(
                                        component,
                                        field,
                                        configurationKey
                                );
                            } catch (ClassNotFoundException | NotFoundException e) {
                                e.printStackTrace();
                            }
                            return null;
                        })
                        .stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList())
                )
                .flatMap(Collection::stream)
                .collect(Collectors.toSet())
                .forEach(field -> {
                    String name = field.getKey().key();
                    name = StringUtils.isEmpty(name) ? field.getField().getName() : name;

                    boolean write = false;

                    if (field.getKey().file()){
                        try {
                            CtMethod injectMethod = JavassistUtils
                                    .getInjectMethod(pool,field.getClasses().getClasses());
                            CtMethod setMethod = JavassistUtils.getSetMethod(field.getClasses().getClasses(),field.getField());
                            injectMethod.insertAfter(String.format(
                                    "$0.%s((%s)$1.getConfigurationFile(\"%s\",%s.class,$0.%s));",
                                    setMethod.getName(),
                                    field.getField().getType().getName(),
                                    field.getKey().key(),
                                    field.getField().getType().getName(),
                                    field.getField().getName()
                            ));
                            write = true;
                        } catch (NotFoundException | CannotCompileException e) {
                            e.printStackTrace();
                        }
                    }

                    Object value = compile.getConfiguration().get(name);
                    if (value != null){
                        try {
                            CtConstructor constructor = field.getClasses().getClasses().getDeclaredConstructor(new CtClass[0]);
                            constructor.insertAfter(String.format(
                                    "$0.%s = \"%s\";\n",
                                    field.getField().getName(),
                                    value
                            ));
                            write = true;
                        } catch (NotFoundException | CannotCompileException e) {
                            e.printStackTrace();
                        }
                    }

                    if (write){
                        field.getClasses().setWrite(true);
                    }
                });

        return components;
    }

}
