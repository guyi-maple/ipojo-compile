package tech.guyi.ipojo.compile.lib.expand.manifest;

import javassist.ClassPool;
import tech.guyi.ipojo.compile.lib.compile.entry.CompileClass;
import tech.guyi.ipojo.compile.lib.configuration.Compile;
import tech.guyi.ipojo.compile.lib.expand.manifest.entry.ListManifest;
import tech.guyi.ipojo.compile.lib.expand.manifest.entry.Manifest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ManifestWriter {

    public void write(ClassPool pool, Set<CompileClass> components, Compile compile, List<ManifestExpand> expands){
        try {
            String directory = compile.getProject().getOutput() + "/META-INF";
            if (new File(directory).mkdirs()){
                PrintWriter writer = new PrintWriter(directory + "/MANIFEST.MF");

                Map<String,Object> compileManifest = new HashMap<>();
                expands.stream()
                        .map(expand -> {
                            try {
                                return expand.execute(pool,components, compile);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            return null;
                        })
                        .filter(Objects::nonNull)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toMap(
                                Manifest::getKey,
                                Function.identity(),
                                this::merge))
                        .values()
                        .stream()
                        .peek(manifest -> {
                            if (manifest instanceof ListManifest && manifest.getKey().equals("Import-Package")){
                                ((ListManifest) manifest).setList(
                                        ((ListManifest) manifest).getList()
                                                .stream()
                                                .filter(value -> compile.getExclude().noneImport(value))
                                                .collect(Collectors.toList())
                                );
                            }
                        })
                        .peek(manifest -> {
                            if (manifest instanceof ListManifest){
                                compileManifest.put(manifest.getKey(),((ListManifest) manifest).getList());
                            }else{
                                compileManifest.put(manifest.getKey(),manifest.getValue());
                            }
                        })
                        .forEach(manifest -> writer.println(String.format("%s: %s",manifest.getKey(),manifest.getValue())));

                compile.setManifestTemplate(compileManifest);

                writer.flush();
                writer.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private Manifest merge(Manifest e1,Manifest e2){
        Manifest one = e1;
        Manifest two = e2;
        if (Integer.compare(e1.getOrder(),e2.getOrder()) == -1){
            one = e2;
            two = e1;
        }

        if (!one.isOverride()){
            return one;
        }

        if (one instanceof ListManifest){
            if (two instanceof ListManifest){
                ((ListManifest) two).getList().forEach(((ListManifest) one)::add);
                ((ListManifest) one).distinct();
            }else {
                ((ListManifest) one).add(two.getValue());
            }
            return one;
        }

        if ((two instanceof ListManifest) && two.isOverride()){
            ((ListManifest) two).add(one.getValue());
        }

        return two;
    }

}
