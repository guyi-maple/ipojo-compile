package top.guyi.iot.ipojo.compile.lib.manifest.defaults;

import top.guyi.iot.ipojo.compile.lib.manifest.Manifest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ListManifest extends Manifest {

    private List<String> list = Collections.emptyList();

    @Override
    public void setValue(String value) {
        this.list = Arrays.asList(value.split(","));
    }

    @Override
    public String getValue() {
        StringBuffer sb = new StringBuffer();

        int length = this.list.size();
        if (length > 2){
            sb.append(this.list.get(0) + ",\n");
            for (int i = 1; i < (length - 1); i++) {
                sb.append(" " + this.list.get(i) + ",\n");
            }
            sb.append(" " + this.list.get(1) + ",.");
        } else if (length == 2){
            sb.append(this.list.get(0) + ",\n");
            sb.append(" " + this.list.get(1) + ",.");
        } else if (length == 1){
            sb.append(" " + this.list.get(0) + ",.");
        }

        return sb.toString();
    }
}
