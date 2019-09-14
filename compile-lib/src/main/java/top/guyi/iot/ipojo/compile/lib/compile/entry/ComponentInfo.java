package top.guyi.iot.ipojo.compile.lib.compile.entry;

import lombok.Data;
import top.guyi.iot.ipojo.compile.lib.compile.entry.ComponentEntry;

import java.util.Set;

@Data
public class ComponentInfo {

    private Set<ComponentEntry> components;

}
