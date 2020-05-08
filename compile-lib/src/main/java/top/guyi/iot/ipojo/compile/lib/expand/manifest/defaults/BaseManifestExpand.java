package top.guyi.iot.ipojo.compile.lib.expand.manifest.defaults;

import javassist.ClassPool;
import top.guyi.iot.ipojo.compile.lib.compile.entry.CompileClass;
import top.guyi.iot.ipojo.compile.lib.configuration.Compile;
import top.guyi.iot.ipojo.compile.lib.expand.manifest.ManifestExpand;
import top.guyi.iot.ipojo.compile.lib.expand.manifest.entry.Manifest;

import java.util.*;

public class BaseManifestExpand implements ManifestExpand {

    @Override
    public List<Manifest> execute(ClassPool pool, Set<CompileClass> components, Compile compile) {
        String version = compile.getProject().getVersion();
        version = version.endsWith("-SNAPSHOT") ? version.replace("-SNAPSHOT","") : version;
        return Arrays.asList(
                new Manifest("Manifest-Version","1.0"),
                new Manifest("Bundle-ManifestVersion","2"),
                new Manifest("Bundle-Name", compile.getName()),
                new Manifest("Bundle-SymbolicName",compile.getSymbolicName()),
                new Manifest("Bundle-Version", version),
                new Manifest("Private-Package",compile.getPackageName())
        );
    }

}
