package tech.guyi.ipojo.compile.lib.expand.manifest.defaults;

import javassist.ClassPool;
import org.apache.commons.io.FileUtils;
import tech.guyi.ipojo.compile.lib.compile.entry.CompileClass;
import tech.guyi.ipojo.compile.lib.configuration.Compile;
import tech.guyi.ipojo.compile.lib.configuration.entry.Dependency;
import tech.guyi.ipojo.compile.lib.enums.CompileType;
import tech.guyi.ipojo.compile.lib.expand.manifest.ManifestExpand;
import tech.guyi.ipojo.compile.lib.expand.manifest.entry.ListManifest;
import tech.guyi.ipojo.compile.lib.maven.MavenHelper;
import tech.guyi.ipojo.compile.lib.expand.manifest.entry.Manifest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
                        if (Files.notExists(Paths.get(path))){
                            MavenHelper.getDependencies(compile.getProject(),dependency);
                        }

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
