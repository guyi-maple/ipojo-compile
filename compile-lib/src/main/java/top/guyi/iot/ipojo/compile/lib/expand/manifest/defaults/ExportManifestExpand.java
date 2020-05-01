package top.guyi.iot.ipojo.compile.lib.expand.manifest.defaults;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.bytecode.annotation.ClassMemberValue;
import top.guyi.iot.ipojo.compile.lib.compile.entry.CompileClass;
import top.guyi.iot.ipojo.compile.lib.configuration.Compile;
import top.guyi.iot.ipojo.compile.lib.cons.AnnotationNames;
import top.guyi.iot.ipojo.compile.lib.expand.manifest.ManifestExpand;
import top.guyi.iot.ipojo.compile.lib.expand.manifest.entry.Manifest;
import top.guyi.iot.ipojo.compile.lib.expand.manifest.entry.ListManifest;
import top.guyi.iot.ipojo.compile.lib.utils.AnnotationUtils;
import top.guyi.iot.ipojo.compile.lib.utils.JavassistUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class ExportManifestExpand implements ManifestExpand {

    @Override
    public List<Manifest> execute(ClassPool pool, Set<CompileClass> components, Compile compile) {
        Set<String> packages = components.stream()
                .filter(component -> AnnotationUtils.getAnnotation(component.getClasses(), AnnotationNames.Service).isPresent())
                .map(component -> AnnotationUtils
                        .getAnnotationValue(component.getClasses(),AnnotationNames.Service,"export")
                        .map(export -> (ClassMemberValue) export)
                        .map(ClassMemberValue::getValue)
                        .map(export -> JavassistUtils.get(pool,export))
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
