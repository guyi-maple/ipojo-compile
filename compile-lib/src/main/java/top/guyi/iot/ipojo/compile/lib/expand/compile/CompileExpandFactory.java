package top.guyi.iot.ipojo.compile.lib.expand.compile;

import top.guyi.iot.ipojo.compile.lib.configuration.Compile;
import top.guyi.iot.ipojo.compile.lib.expand.compile.defaults.configuration.ConfigurationExpand;
import top.guyi.iot.ipojo.compile.lib.expand.compile.defaults.event.EventExpand;
import top.guyi.iot.ipojo.compile.lib.expand.compile.defaults.log.LoggerExpand;
import top.guyi.iot.ipojo.compile.lib.expand.compile.defaults.service.BundleServiceReferenceExpand;
import top.guyi.iot.ipojo.compile.lib.expand.compile.defaults.service.ServiceRegisterExpand;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class CompileExpandFactory {

    private List<CompileExpand> expands;

    public CompileExpandFactory(){
        this.expands = new LinkedList<>();

        this.add(new ConfigurationExpand())
                .add(new EventExpand())
                .add(new LoggerExpand())
                .add(new BundleServiceReferenceExpand())
                .add(new ServiceRegisterExpand());
    }

    public CompileExpandFactory add(CompileExpand expand){
        this.expands.add(expand);
        return this;
    }

    public List<CompileExpand> get(Compile compile){
        return this.expands.stream()
                .filter(expand -> expand.check(compile))
                .collect(Collectors.toList());
    }

}
