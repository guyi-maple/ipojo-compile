package tech.guyi.ipojo.compile.lib.configuration.entry;

import lombok.Data;
import tech.guyi.ipojo.compile.lib.utils.MavenUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;

@Data
public class Dependency {

    private String groupId;
    private String artifactId;
    private String version;
    private String scope;

    public Dependency(String groupId, String artifactId, String version, String scope) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.scope = scope;
    }

    public String getScope() {
        return Optional.ofNullable(this.scope).orElse("compile");
    }

    public String getFileName(){
        return String.format("%s-%s.jar",artifactId,version);
    }

    public String getName(){
        return String.format("%s:%s:%s",groupId,artifactId,version);
    }

    public Optional<String> get(Project project){
        return MavenUtils.get(project,this);
    }

    public Optional<URL> getURL(Project project) {
        return this.get(project)
                .map(path -> {
                    try {
                        return new URL(String.format("file:///%s",path));
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                    return null;
                });
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
