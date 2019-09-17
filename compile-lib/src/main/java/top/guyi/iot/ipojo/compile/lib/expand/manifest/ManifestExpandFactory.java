package top.guyi.iot.ipojo.compile.lib.expand.manifest;

import javassist.ClassPool;
import top.guyi.iot.ipojo.compile.lib.compile.entry.CompileClass;
import top.guyi.iot.ipojo.compile.lib.configuration.Compile;
import top.guyi.iot.ipojo.compile.lib.expand.manifest.defaults.*;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
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
                .collect(Collectors.toList());
    }

    public void write(ClassPool pool,Compile compile, Set<CompileClass> components){
        this.writer.write(pool,components,compile,this.get(compile));
    }

}
