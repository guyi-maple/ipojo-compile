package top.guyi.iot.ipojo.compile.expand.manifest;

import javassist.ClassPool;
import top.guyi.iot.ipojo.compile.lib.compile.entry.CompileClass;
import top.guyi.iot.ipojo.compile.lib.configuration.CompileInfo;
import top.guyi.iot.ipojo.compile.lib.project.configuration.ProjectInfo;
import top.guyi.iot.ipojo.compile.lib.expand.ManifestExpand;
import top.guyi.iot.ipojo.compile.lib.manifest.Manifest;
import top.guyi.iot.ipojo.compile.lib.manifest.defaults.ListManifest;

import java.util.*;
import java.util.stream.Collectors;

public class TemplateManifestExpand implements ManifestExpand {

    @Override
    public List<Manifest> execute(ClassPool pool, Set<CompileClass> components, CompileInfo compileInfo, ProjectInfo projectInfo) {
        return compileInfo.getManifestTemplate().entrySet().stream()
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
