package top.guyi.iot.ipojo.compile.expand.manifest;

import javassist.ClassPool;
import top.guyi.iot.ipojo.compile.lib.compile.entry.CompileClass;
import top.guyi.iot.ipojo.compile.lib.configuration.Compile;
import top.guyi.iot.ipojo.compile.lib.expand.ManifestExpand;
import top.guyi.iot.ipojo.compile.lib.manifest.Manifest;

import java.util.*;

public class BaseManifestExpand implements ManifestExpand {

    @Override
    public List<Manifest> execute(ClassPool pool, Set<CompileClass> components, Compile compile) {
        return Arrays.asList(
                new Manifest("Manifest-Version","1.0"),
                new Manifest("Bundle-ManifestVersion","2"),
                new Manifest("Bundle-Name", compile.getName()),
                new Manifest("Bundle-SymbolicName",compile.getSymbolicName()),
                new Manifest("Bundle-Version",compile.getProject().getVersion()),
                new Manifest("Private-Package",compile.getPackageName())
        );
    }

}
