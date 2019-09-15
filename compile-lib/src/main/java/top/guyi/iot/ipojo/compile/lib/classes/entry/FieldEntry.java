package top.guyi.iot.ipojo.compile.lib.classes.entry;

import javassist.CtField;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class FieldEntry {

    private CtField field;
    private String name;
    private boolean equals;

    public FieldEntry(CtField field,boolean equals) {
        this.field = field;
        this.equals = equals;
    }

    public FieldEntry(CtField field, String name,boolean equals) {
        this.field = field;
        this.name = name;
        this.equals = equals;
    }
}
