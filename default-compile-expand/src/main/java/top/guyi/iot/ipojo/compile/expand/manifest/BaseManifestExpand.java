package top.guyi.iot.ipojo.compile.expand.manifest;

import top.guyi.iot.ipojo.compile.lib.compile.entry.CompileInfo;
import top.guyi.iot.ipojo.compile.lib.compile.entry.ProjectInfo;
import top.guyi.iot.ipojo.compile.lib.expand.ManifestExpand;
import top.guyi.iot.ipojo.compile.lib.manifest.Manifest;

import java.util.Collections;
import java.util.List;

public class BaseManifestExpand implements ManifestExpand {

    @Override
    public List<Manifest> execute(CompileInfo compileInfo, ProjectInfo projectInfo) {
        Manifest manifest = new Manifest();
        manifest.setKey("Bundle-Version");
        manifest.setValue(projectInfo.getVersion());
        return Collections.singletonList(manifest);
    }

}
