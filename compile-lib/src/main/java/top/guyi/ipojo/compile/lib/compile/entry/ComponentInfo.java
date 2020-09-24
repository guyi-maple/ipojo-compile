package top.guyi.ipojo.compile.lib.compile.entry;

import lombok.Data;
import top.guyi.ipojo.compile.lib.expand.compile.defaults.configuration.entry.ConfigurationKeyEntry;

import java.util.Set;

@Data
public class ComponentInfo {

    private String name;
    private Set<ComponentEntry> components;
    private Set<ComponentEntry> useComponents;
    private Set<ConfigurationKeyEntry> configurations;

}
