package top.guyi.iot.ipojo.compile.lib.compile.entry;

import lombok.Data;

@Data
public class ComponentEntry {

    private String type = "classes";
    private String classes;

    public ComponentEntry(String classes) {
        this.classes = classes;
    }

    public ComponentEntry(String type, String classes) {
        this.type = type;
        this.classes = classes;
    }
}
