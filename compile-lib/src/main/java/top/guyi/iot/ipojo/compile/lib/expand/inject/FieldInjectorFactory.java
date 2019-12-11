package top.guyi.iot.ipojo.compile.lib.expand.inject;

import javassist.ClassPool;
import top.guyi.iot.ipojo.compile.lib.classes.entry.FieldEntry;
import top.guyi.iot.ipojo.compile.lib.expand.inject.defaults.ListFieldInjector;
import top.guyi.iot.ipojo.compile.lib.expand.inject.defaults.MapFieldInjector;
import top.guyi.iot.ipojo.compile.lib.expand.inject.defaults.ObjectInjector;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class FieldInjectorFactory {

    private List<FieldInjector> injectors;
    private FieldInjector injector = new ObjectInjector();

    public FieldInjectorFactory(){
        this.injectors = new LinkedList<>();

        this.injectors.add(new ListFieldInjector());
        this.injectors.add(new MapFieldInjector());
    }

    public FieldInjector get(FieldEntry field, ClassPool pool){
        return this.injectors.stream()
                .filter(injectors -> injectors.check(field,pool))
                .min(Comparator.comparingInt(FieldInjector::order))
                .orElse(this.injector);
    }

}
