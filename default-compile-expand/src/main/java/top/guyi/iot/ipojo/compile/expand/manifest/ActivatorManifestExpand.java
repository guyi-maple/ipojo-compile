package top.guyi.iot.ipojo.compile.expand.manifest;

import javassist.ClassPool;
import top.guyi.iot.ipojo.compile.lib.compile.entry.CompileClass;
import top.guyi.iot.ipojo.compile.lib.configuration.CompileInfo;
import top.guyi.iot.ipojo.compile.lib.project.configuration.ProjectInfo;
import top.guyi.iot.ipojo.compile.lib.enums.CompileType;
import top.guyi.iot.ipojo.compile.lib.expand.ManifestExpand;
import top.guyi.iot.ipojo.compile.lib.manifest.Manifest;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class ActivatorManifestExpand implements ManifestExpand {

    @Override
    public List<Manifest> execute(ClassPool pool, Set<CompileClass> components, CompileInfo compileInfo, ProjectInfo projectInfo) {
        return Optional.ofNullable(compileInfo.getActivator())
                .filter(value -> compileInfo.getType() == CompileType.BUNDLE)
                .map(value -> Collections.singletonList(
                        new Manifest("Bundle-Activator",value)
                ))
                .orElseGet(Collections::emptyList);
    }

}
