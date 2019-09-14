package top.guyi.iot.ipojo.compile.expand.manifest;

import javassist.ClassPool;
import top.guyi.iot.ipojo.compile.lib.compile.entry.CompileClass;
import top.guyi.iot.ipojo.compile.lib.configuration.CompileInfo;
import top.guyi.iot.ipojo.compile.lib.project.configuration.ProjectInfo;
import top.guyi.iot.ipojo.compile.lib.expand.ManifestExpand;
import top.guyi.iot.ipojo.compile.lib.manifest.Manifest;

import java.util.*;

public class BaseManifestExpand implements ManifestExpand {

    @Override
    public List<Manifest> execute(ClassPool pool, Set<CompileClass> components, CompileInfo compileInfo, ProjectInfo projectInfo) {
        return Arrays.asList(
                new Manifest("Manifest-Version","1.0"),
                new Manifest("Bundle-ManifestVersion","2"),
                new Manifest("Bundle-Name",compileInfo.getName()),
                new Manifest("Bundle-SymbolicName",Optional
                        .ofNullable(compileInfo.getSymbolicName())
                        .orElseGet(projectInfo::getArtifactId)),
                new Manifest("Bundle-Version",projectInfo.getVersion())
        );
    }

}
