package top.guyi.iot.ipojo.compile.expand.manifest;

import javassist.ClassPool;
import org.apache.commons.io.FileUtils;
import top.guyi.iot.ipojo.compile.expand.helper.ImportHelper;
import top.guyi.iot.ipojo.compile.lib.compile.entry.CompileClass;
import top.guyi.iot.ipojo.compile.lib.configuration.Compile;
import top.guyi.iot.ipojo.compile.lib.configuration.entry.Dependency;
import top.guyi.iot.ipojo.compile.lib.enums.CompileType;
import top.guyi.iot.ipojo.compile.lib.expand.ManifestExpand;
import top.guyi.iot.ipojo.compile.lib.manifest.Manifest;
import top.guyi.iot.ipojo.compile.lib.manifest.defaults.ListManifest;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class DependencyManifestExpand implements ManifestExpand {

    @Override
    public List<Manifest> execute(ClassPool pool, Set<CompileClass> components, Compile compile) {
        List<Manifest> manifests = new LinkedList<>();

        if (compile.getType() == CompileType.BUNDLE){
            Set<Dependency> dependencies = compile.getProject().getDependencies()
                    .stream()
                    .filter(dependency -> compile.getExclude().noneDependencyCopy(dependency))
                    .collect(Collectors.toSet());

            if (!dependencies.isEmpty()){
                ListManifest manifest = new ListManifest();
                manifest.setKey("Bundle-ClassPath");
                manifest.setEndString(",.");

                File root = new File(compile.getProject().getOutput() + "/lib");
                if (root.mkdirs()){
                    for (Dependency dependency : dependencies) {
                        File target = new File(root.getAbsolutePath() + "/" + dependency.getFileName());
                        File source = new File(dependency.getPath());
                        try {
                            FileUtils.copyFile(source,target);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        manifest.add("lib/" + dependency.getFileName());
                    }
                    manifests.add(manifest);
                }
            }
        }

        Set<String> importPackages = ImportHelper
                .getDependencyImportPackages(compile.getProject().getDependencies())
                .stream()
                .filter(packageName -> compile.getExclude().noneImport(packageName))
                .collect(Collectors.toSet());
        if (!importPackages.isEmpty()){
            ListManifest manifest = new ListManifest();
            manifest.setKey("Import-Package");
            importPackages.forEach(manifest::add);
            manifests.add(manifest);
        }

        return manifests;
    }

}
