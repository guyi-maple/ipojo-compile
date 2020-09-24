package top.guyi.ipojo.compile.lib.compile.entry;

import lombok.Data;

@Data
public class ComponentEntry {

    private String type = "classes";
    private String classes;
    private boolean proxy = false;

    public ComponentEntry(String classes) {
        this.classes = classes;
    }

    public ComponentEntry(String type, String classes) {
        this.type = type;
        this.classes = classes;
    }

    public ComponentEntry(String type, String classes, boolean proxy) {
        this.type = type;
        this.classes = classes;
        this.proxy = proxy;
    }
}
