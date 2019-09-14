package top.guyi.iot.ipojo.compile.lib.classes.entry;

import javassist.CtField;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class FieldEntry {

    private CtField field;
    private String name;

    public FieldEntry(CtField field) {
        this.field = field;
    }

    public FieldEntry(CtField field, String name) {
        this.field = field;
        this.name = name;
    }
}
