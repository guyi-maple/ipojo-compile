package top.guyi.iot.ipojo.compile.lib.expand.manifest.defaults;

import javassist.ClassPool;
import org.apache.commons.io.FileUtils;
import top.guyi.iot.ipojo.compile.lib.compile.entry.CompileClass;
import top.guyi.iot.ipojo.compile.lib.configuration.Compile;
import top.guyi.iot.ipojo.compile.lib.configuration.entry.Dependency;
import top.guyi.iot.ipojo.compile.lib.enums.CompileType;
import top.guyi.iot.ipojo.compile.lib.expand.manifest.ManifestExpand;
import top.guyi.iot.ipojo.compile.lib.expand.manifest.entry.Manifest;
import top.guyi.iot.ipojo.compile.lib.expand.manifest.entry.ListManifest;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class DependencyManifestExpand implements ManifestExpand {

    @Override
    public boolean check(Compile compile) {
        return compile.getType() == CompileType.BUNDLE;
    }

    @Override
    public List<Manifest> execute(ClassPool pool, Set<CompileClass> components, Compile compile) {
        List<Manifest> manifests = new LinkedList<>();

        Set<Dependency> dependencies = compile.getProject().getDependencies()
                .stream()
                .filter(dependency -> compile.getExclude().noneDependencyCopy(dependency))
                .filter(dependency -> compile.getExclude().noneDependencyScope(dependency))
                .collect(Collectors.toSet());

        if (!dependencies.isEmpty()){
            ListManifest manifest = new ListManifest();
            manifest.setKey("Bundle-ClassPath");
            manifest.setEndString(",.");

            File root = new File(compile.getProject().getOutput() + "/lib");
            if (root.mkdirs()){
                for (Dependency dependency : dependencies) {
                    dependency.get(compile.getProject()).ifPresent(path -> {
                        File target = new File(root.getAbsolutePath() + "/" + dependency.getFileName());
                        File source = new File(path);
                        try {
                            FileUtils.copyFile(source,target);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        manifest.add("lib/" + dependency.getFileName());
                    });
                }
                manifests.add(manifest);
            }
        }

        return manifests;
    }

}
