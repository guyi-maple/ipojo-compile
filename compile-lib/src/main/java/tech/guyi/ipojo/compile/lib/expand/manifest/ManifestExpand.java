package tech.guyi.ipojo.compile.lib.expand.manifest;

import javassist.ClassPool;
import tech.guyi.ipojo.compile.lib.compile.entry.CompileClass;
import tech.guyi.ipojo.compile.lib.configuration.Compile;
import tech.guyi.ipojo.compile.lib.expand.manifest.entry.Manifest;

import java.util.List;
import java.util.Set;

public interface ManifestExpand {

    default int order(){
        return 999;
    }

    default boolean check(Compile compile){
        return true;
    }

    List<Manifest> execute(ClassPool pool, Set<CompileClass> components, Compile compile) throws Exception;

}
