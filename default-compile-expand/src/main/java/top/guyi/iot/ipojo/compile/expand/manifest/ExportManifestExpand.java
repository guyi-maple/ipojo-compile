package top.guyi.iot.ipojo.compile.expand.manifest;

import javassist.ClassPool;
import top.guyi.iot.ipojo.application.osgi.service.annotation.Service;
import top.guyi.iot.ipojo.compile.lib.compile.entry.CompileClass;
import top.guyi.iot.ipojo.compile.lib.configuration.CompileInfo;
import top.guyi.iot.ipojo.compile.lib.expand.ManifestExpand;
import top.guyi.iot.ipojo.compile.lib.manifest.Manifest;
import top.guyi.iot.ipojo.compile.lib.manifest.defaults.ListManifest;
import top.guyi.iot.ipojo.compile.lib.project.configuration.ProjectInfo;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class ExportManifestExpand implements ManifestExpand {

    @Override
    public List<Manifest> execute(ClassPool pool, Set<CompileClass> components, CompileInfo compileInfo, ProjectInfo projectInfo) {
        Set<String> packages = components.stream()
                .map(component -> {
                    try {
                        Service service = (Service) component.getClasses().getAnnotation(Service.class);
                        if (service != null){
                            return service.value().getPackage().getName();
                       }
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                    return null;
                })
                .filter(Objects::nonNull)
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
