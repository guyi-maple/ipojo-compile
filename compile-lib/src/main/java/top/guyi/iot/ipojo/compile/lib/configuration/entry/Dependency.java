package top.guyi.iot.ipojo.compile.lib.configuration.entry;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Objects;

@Data
@AllArgsConstructor
public class Dependency {

    private String repository;
    private String groupId;
    private String artifactId;
    private String version;
    private String scope;

    public String getFileName(){
        return String.format("%s-%s.jar",artifactId,version);
    }

    public String getName(){
        return String.format("%s:%s:%s",groupId,artifactId,version);
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Dependency that = (Dependency) o;
        return Objects.equals(getName(), that.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName());
    }
}
