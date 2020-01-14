package top.guyi.iot.ipojo.compile.lib.configuration.entry;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

@Data
@AllArgsConstructor
public class Dependency {

    private String groupId;
    private String artifactId;
    private String version;
    private String scope;
    private String url;

    public Dependency(String groupId, String artifactId, String version, String scope) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.scope = scope;
    }

    public String getFileName(){
        return String.format("%s-%s.jar",artifactId,version);
    }

    public String getName(){
        return String.format("%s:%s:%s",groupId,artifactId,version);
    }

    public String get(Project project){
        if (this.url == null){
            this.url = String.format(
                    "%s/%s/%s/%s/%s-%s.jar",
                    project.getRepository(),
                    groupId.replaceAll("\\.", "/"),
                    artifactId,
                    version,
                    artifactId,
                    version
            );
        }
        return url;
    }

    public URL getURL(Project project) throws MalformedURLException {
        if (this.url == null){
            this.url = String.format(
                    "%s/%s/%s/%s/%s-%s.jar",
                    project.getRepository(),
                    groupId.replaceAll("\\.", "/"),
                    artifactId,
                    version,
                    artifactId,
                    version
            );
        }
        return url.contains(":") ? new URL(url) : new URL(String.format("file:///%s",url));
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
