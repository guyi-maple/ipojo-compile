package tech.guyi.ipojo.compile.lib.expand.manifest.defaults;

import javassist.ClassPool;
import tech.guyi.ipojo.compile.lib.compile.entry.CompileClass;
import tech.guyi.ipojo.compile.lib.configuration.Compile;
import tech.guyi.ipojo.compile.lib.expand.manifest.ManifestExpand;
import tech.guyi.ipojo.compile.lib.expand.manifest.entry.ListManifest;
import tech.guyi.ipojo.compile.lib.expand.manifest.entry.Manifest;

import java.util.*;
import java.util.stream.Collectors;

public class TemplateManifestExpand implements ManifestExpand {

    @Override
    public int order() {
        return 900;
    }

    @Override
    public List<Manifest> execute(ClassPool pool, Set<CompileClass> components, Compile compile) {
        return compile.getManifestTemplate().entrySet().stream()
                .map(this::convert)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private boolean checkValue(Object value){
        return (value instanceof String) || (value instanceof Number) || (value instanceof Boolean);
    }

    private Manifest convert(Map.Entry<String,Object> e){
        if (this.checkValue(e.getValue())){
            return new Manifest(e.getKey(),e.getValue().toString());
        }

        if (e.getValue() instanceof List){
            ListManifest manifest = new ListManifest();
            manifest.setKey(e.getKey());
            ((List<String>) e.getValue()).forEach(manifest::add);
            return manifest;
        }

        if (e.getValue() instanceof Map){
            Map<String,Object> value = (Map<String,Object>) e.getValue();
            Manifest manifest;
            if (value.get("value") instanceof List){
                manifest = new ListManifest();
                ((List) value.get("value")).forEach(line -> ((ListManifest)manifest).add(line.toString()));
                ((ListManifest)manifest).setEndString(value.getOrDefault("endString","").toString());
            } else {
                manifest = new Manifest();
                manifest.setValue(value.getOrDefault(value,"").toString());
            }
            manifest.setKey(e.getKey());
            manifest.setOrder(Integer.parseInt(value.getOrDefault("order",998).toString()));
            manifest.setOverride(Boolean.parseBoolean(value.getOrDefault("override",true).toString()));
            return manifest;
        }
        return null;
    }

}
