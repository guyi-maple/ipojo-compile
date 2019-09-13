package top.guyi.iot.ipojo.compile.lib.manifest;

import lombok.Data;

@Data
public class Manifest {

    private String key;
    private String value;

    public String format(){
        return String.format("%s: %s",this.getKey(),this.getValue());
    }

}
