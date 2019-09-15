package top.guyi.iot.ipojo.compile.lib.expand;

import javassist.ClassPool;
import top.guyi.iot.ipojo.compile.lib.compile.entry.CompileClass;
import top.guyi.iot.ipojo.compile.lib.configuration.Compile;
import top.guyi.iot.ipojo.compile.lib.manifest.Manifest;

import java.util.List;
import java.util.Set;

public interface ManifestExpand {

    default int order(){
        return 999;
    }

    List<Manifest> execute(ClassPool pool, Set<CompileClass> components, Compile compile) throws Exception;

}
