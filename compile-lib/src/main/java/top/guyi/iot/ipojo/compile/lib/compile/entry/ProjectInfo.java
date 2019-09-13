package top.guyi.iot.ipojo.compile.lib.compile.entry;

import lombok.Data;

import java.util.Map;

@Data
public class ProjectInfo {

    private String finalName;
    private String version;
    private String groupId;
    private String artifactId;
    private Map<String,String> properties;

}
