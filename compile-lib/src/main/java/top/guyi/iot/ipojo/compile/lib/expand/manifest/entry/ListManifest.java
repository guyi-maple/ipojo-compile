package top.guyi.iot.ipojo.compile.lib.expand.manifest.entry;

import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class ListManifest extends Manifest {

    @Setter
    private String endString = "";
    @Getter
    private List<String> list = new LinkedList<>();

    public ListManifest add(String line){
        list.add(line);
        return this;
    }

    public void distinct(){
        this.list = this.list.stream().distinct().collect(Collectors.toList());
    }

    @Override
    public void setValue(String value) {
        this.list = Arrays.asList(value.split(","));
    }

    @Override
    public String getValue() {
        StringBuffer sb = new StringBuffer();

        int length = this.list.size();
        if (length > 2){
            sb.append(this.list.get(0)).append(",\n");
            for (int i = 1; i < (length - 1); i++) {
                sb.append(" ").append(this.list.get(i)).append(",\n");
            }
            sb.append(" ").append(this.list.get(length - 1)).append(endString);
        } else if (length == 2){
            sb.append(this.list.get(0)).append(",\n");
            sb.append(" ").append(this.list.get(1)).append(endString);
        } else if (length == 1){
            sb.append(this.list.get(0)).append(endString);
        }

        return sb.toString();
    }
}
