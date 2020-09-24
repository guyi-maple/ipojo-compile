package top.guyi.ipojo.compile.lib.expand.manifest.entry;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Manifest {

    private boolean override = true;
    private int order = 999;
    private String key;
    private String value;

    public Manifest(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String format(){
        return String.format("%s: %s",this.getKey(),this.getValue());
    }

}
