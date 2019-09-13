package top.guyi.iot.ipojo.compile.expand.service;

import top.guyi.iot.ipojo.application.osgi.log.Log;
import top.guyi.iot.ipojo.application.osgi.log.AbstractLoggerRepository;
import top.guyi.iot.ipojo.compile.lib.compile.entry.CompileInfo;
import top.guyi.iot.ipojo.compile.lib.expand.CompileExpand;
import top.guyi.iot.ipojo.compile.lib.utils.JavassistUtils;
import javassist.*;

import java.util.Arrays;
import java.util.Set;

public class LoggerExpand implements CompileExpand {

    @Override
    public Set<CtClass> execute(ClassPool pool, String path, CompileInfo compileInfo, Set<CtClass> components) throws Exception {
        CtClass repository = pool.get(AbstractLoggerRepository.class.getName());

//        CtMethod referenceMethod = new CtMethod(CtClass.voidType,"awaitLoggerFactory",new CtClass[]{
//                pool.get(LoggerFactory.class.getName())
//        },repository);
//        referenceMethod.setBody("{$0.createLogger($$);}");
//        AnnotationsAttribute attribute = new AnnotationsAttribute(repository.getClassFile().getConstPool(),AnnotationsAttribute.visibleTag);
//        Annotation annotation = new Annotation(BundleServiceReference.class.getName(),repository.getClassFile().getConstPool());
//        annotation.addMemberValue("value",new ClassMemberValue(LoggerFactory.class.getName(),repository.getClassFile().getConstPool()));
//        attribute.addAnnotation(annotation);
//        referenceMethod.getMethodInfo().addAttribute(attribute);
//        repository.addMethod(referenceMethod);

        components.add(repository);

        components.forEach(component -> {
            StringBuilder sb = new StringBuilder();
            Arrays.stream(component.getDeclaredFields())
                    .filter(field -> field.hasAnnotation(Log.class))
                    .forEach(field -> {
                        try {
                            Log log = (Log) field.getAnnotation(Log.class);
                            CtMethod setMethod = JavassistUtils.getSetMethod(component,field);
                            sb.append(String.format(
                                    "$0.%s(%s.get(\"%s\"));\n",
                                    setMethod.getName(),
                                    AbstractLoggerRepository.class.getName(),
                                    log.value())
                            );
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                    });
            try {
                CtMethod inject = JavassistUtils.getInjectMethod(pool,component);
                inject.insertAfter(sb.toString());
            } catch (CannotCompileException e) {
                e.printStackTrace();
            } catch (NotFoundException e) {
            }
        });

        return components;
    }

}
