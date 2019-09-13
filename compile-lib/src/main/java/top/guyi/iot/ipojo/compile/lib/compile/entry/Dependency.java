package top.guyi.iot.ipojo.compile.lib.compile.entry;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Dependency {

    private String repository;
    private String groupId;
    private String artifactId;
    private String version;
    private String scope;

    public String getPath(){
        return String.format(
                "%s/%s/%s/%s/%s-%s.jar",
                repository,
                groupId.replaceAll("\\.", "/"),
                artifactId,
                version,
                artifactId,
                version
        );
    }

}
