package top.guyi.iot.ipojo.compile.lib.compile.entry;

import lombok.Data;

import java.util.Set;

@Data
public class ComponentInfo {

    private String name;
    private Set<ComponentEntry> components;
    private Set<ComponentEntry> useComponents;

}
