package tech.guyi.ipojo.compile.lib.expand.manifest;

import javassist.ClassPool;
import tech.guyi.ipojo.compile.lib.compile.entry.CompileClass;
import tech.guyi.ipojo.compile.lib.configuration.Compile;
import tech.guyi.ipojo.compile.lib.expand.manifest.defaults.*;

import java.util.*;
import java.util.stream.Collectors;

public class ManifestExpandFactory {

    private List<ManifestExpand> expands;
    private ManifestWriter writer;

    public ManifestExpandFactory() {
        this.writer = new ManifestWriter();

        this.expands = new LinkedList<>();
        this.add(new ActivatorManifestExpand())
                .add(new BaseManifestExpand())
                .add(new DependencyManifestExpand())
                .add(new ExportManifestExpand())
                .add(new TemplateManifestExpand());
    }

    private ManifestExpandFactory add(ManifestExpand expand){
        this.expands.add(expand);
        return this;
    }

    public List<ManifestExpand> get(Compile compile){
        return this.expands.stream()
                .filter(expand -> expand.check(compile))
                .sorted(Comparator.comparingInt(ManifestExpand::order))
                .collect(Collectors.toList());
    }

    public void write(ClassPool pool,Compile compile, Set<CompileClass> components){
        this.writer.write(pool,components,compile,this.get(compile));
    }

}
