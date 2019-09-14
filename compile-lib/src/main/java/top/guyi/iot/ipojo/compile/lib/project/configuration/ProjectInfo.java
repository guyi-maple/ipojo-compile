package top.guyi.iot.ipojo.compile.lib.project.configuration;

import lombok.Data;
import top.guyi.iot.ipojo.compile.lib.project.entry.Dependency;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

@Data
public class ProjectInfo {

    private String baseDir;
    private String sourceDir;
    private String finalName;
    private String version;
    private String groupId;
    private String artifactId;
    private Map<String,String> properties;
    private Set<Dependency> dependencies = Collections.emptySet();

}
