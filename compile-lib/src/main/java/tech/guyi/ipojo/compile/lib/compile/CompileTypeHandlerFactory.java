package tech.guyi.ipojo.compile.lib.compile;

import tech.guyi.ipojo.compile.lib.compile.defaults.BundleTypeHandler;
import tech.guyi.ipojo.compile.lib.compile.defaults.ComponentTypeHandler;
import tech.guyi.ipojo.compile.lib.configuration.Compile;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class CompileTypeHandlerFactory {

    private List<CompileTypeHandler> handlers;

    public CompileTypeHandlerFactory(){
        this.handlers = new LinkedList<>();

        this.add(new BundleTypeHandler())
                .add(new ComponentTypeHandler());
    }

    public CompileTypeHandlerFactory add(CompileTypeHandler handler){
        this.handlers.add(handler);
        return this;
    }

    public List<CompileTypeHandler> get(Compile compile){
        return this.handlers.stream()
                .filter(handler -> handler.check(compile))
                .sorted(Comparator.comparingInt(CompileTypeHandler::order))
                .collect(Collectors.toList());
    }


}
