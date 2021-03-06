package tech.guyi.ipojo.compile.lib.expand.manifest.defaults;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.bytecode.annotation.BooleanMemberValue;
import javassist.bytecode.annotation.ClassMemberValue;
import tech.guyi.ipojo.compile.lib.compile.entry.CompileClass;
import tech.guyi.ipojo.compile.lib.configuration.Compile;
import tech.guyi.ipojo.compile.lib.cons.AnnotationNames;
import tech.guyi.ipojo.compile.lib.expand.manifest.ManifestExpand;
import tech.guyi.ipojo.compile.lib.expand.manifest.entry.ListManifest;
import tech.guyi.ipojo.compile.lib.utils.AnnotationUtils;
import tech.guyi.ipojo.compile.lib.utils.JavassistUtils;
import tech.guyi.ipojo.compile.lib.expand.manifest.entry.Manifest;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class ExportManifestExpand implements ManifestExpand {

    @Override
    public List<Manifest> execute(ClassPool pool, Set<CompileClass> components, Compile compile) {
        Set<String> packages = components.stream()
                .map(component -> AnnotationUtils
                        .getAnnotation(component.getClasses(), AnnotationNames.Service)
                        .filter(annotation -> AnnotationUtils.getAnnotationValue(annotation,"export")
                                .map(export -> (BooleanMemberValue) export)
                                .map(BooleanMemberValue::getValue)
                                .orElse(true))
                        .flatMap(annotation -> AnnotationUtils.getAnnotationValue(annotation,"value"))
                        .map(value -> (ClassMemberValue) value)
                        .map(ClassMemberValue::getValue)
                        .map(value -> JavassistUtils.get(pool,value))
                        .map(CtClass::getPackageName)
                        .orElse(null))
                .filter(Objects::nonNull)
                .filter(packageName -> compile.getExclude().noneExport(packageName))
                .collect(Collectors.toSet());

        if (packages.isEmpty()){
            return Collections.emptyList();
        }

        ListManifest manifest = new ListManifest();
        manifest.setKey("Export-Package");
        packages.forEach(manifest::add);
        return Collections.singletonList(manifest);
    }

}
