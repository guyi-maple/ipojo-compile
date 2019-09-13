package top.guyi.iot.ipojo.compile.lib.expand;

import top.guyi.iot.ipojo.compile.lib.compile.entry.CompileInfo;
import top.guyi.iot.ipojo.compile.lib.compile.entry.ProjectInfo;
import top.guyi.iot.ipojo.compile.lib.manifest.Manifest;

import java.util.List;

public interface ManifestExpand {

    default int order(){
        return 999;
    }

    List<Manifest> execute(CompileInfo compileInfo, ProjectInfo projectInfo);

}
