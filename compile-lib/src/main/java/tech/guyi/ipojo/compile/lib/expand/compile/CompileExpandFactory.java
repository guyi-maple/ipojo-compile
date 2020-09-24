package tech.guyi.ipojo.compile.lib.expand.compile;

import tech.guyi.ipojo.compile.lib.expand.compile.defaults.coap.CoapExpand;
import tech.guyi.ipojo.compile.lib.expand.compile.defaults.configuration.ConfigurationExpand;
import tech.guyi.ipojo.compile.lib.expand.compile.defaults.event.EventExpand;
import tech.guyi.ipojo.compile.lib.expand.compile.defaults.log.LoggerExpand;
import tech.guyi.ipojo.compile.lib.expand.compile.defaults.service.BundleServiceReferenceExpand;
import tech.guyi.ipojo.compile.lib.expand.compile.defaults.service.ServiceRegisterExpand;
import tech.guyi.ipojo.compile.lib.expand.compile.defaults.stream.AwaiterExpand;
import tech.guyi.ipojo.compile.lib.expand.compile.defaults.timer.TimeCompileExpand;
import tech.guyi.ipojo.compile.lib.configuration.Compile;

import java.util.Comparator;
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
                .add(new ServiceRegisterExpand())
                .add(new CoapExpand())
                .add(new AwaiterExpand())
                .add(new TimeCompileExpand());
    }

    public CompileExpandFactory add(CompileExpand expand){
        this.expands.add(expand);
        return this;
    }

    public List<CompileExpand> get(Compile compile){
        return this.expands.stream()
                .filter(expand -> expand.check(compile))
                .sorted(Comparator.comparingInt(CompileExpand::order))
                .collect(Collectors.toList());
    }

}
