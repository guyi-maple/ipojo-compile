package top.guyi.iot.ipojo.compile.expand.manifest;

import javassist.ClassPool;
import top.guyi.iot.ipojo.compile.lib.compile.entry.CompileClass;
import top.guyi.iot.ipojo.compile.lib.configuration.CompileInfo;
import top.guyi.iot.ipojo.compile.lib.expand.ManifestExpand;
import top.guyi.iot.ipojo.compile.lib.manifest.Manifest;
import top.guyi.iot.ipojo.compile.lib.project.configuration.ProjectInfo;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class VersionManifestExpand implements ManifestExpand {

    @Override
    public List<Manifest> execute(ClassPool pool, Set<CompileClass> components, CompileInfo compileInfo, ProjectInfo projectInfo) {
        compileInfo.setVersion(Optional.ofNullable(compileInfo.getVersion()).orElse(projectInfo.getVersion()));
        return Collections.singletonList(new Manifest("Bundle-Version",compileInfo.getVersion()));
    }

}
