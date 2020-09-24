package tech.guyi.ipojo.compile.lib.expand.manifest.defaults;

import javassist.ClassPool;
import tech.guyi.ipojo.compile.lib.compile.entry.CompileClass;
import tech.guyi.ipojo.compile.lib.configuration.Compile;
import tech.guyi.ipojo.compile.lib.enums.CompileType;
import tech.guyi.ipojo.compile.lib.expand.manifest.ManifestExpand;
import tech.guyi.ipojo.compile.lib.expand.manifest.entry.Manifest;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class ActivatorManifestExpand implements ManifestExpand {

    @Override
    public List<Manifest> execute(ClassPool pool, Set<CompileClass> components, Compile compile) {
        return Optional.ofNullable(compile.getActivator())
                .filter(value -> compile.getType() == CompileType.BUNDLE)
                .map(value -> Collections.singletonList(
                        new Manifest("Bundle-Activator",value)))
                .orElseGet(Collections::emptyList);
    }

}
