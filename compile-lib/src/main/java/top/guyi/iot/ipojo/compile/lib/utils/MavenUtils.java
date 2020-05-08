package top.guyi.iot.ipojo.compile.lib.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import top.guyi.iot.ipojo.compile.lib.configuration.entry.Dependency;
import top.guyi.iot.ipojo.compile.lib.configuration.entry.Project;
import top.guyi.iot.ipojo.compile.lib.maven.MavenHelper;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

@Data
@NoArgsConstructor
@AllArgsConstructor
class SnapshotVersion {
    private long lastUpdated;
    private String value;
}

/**
 * @author guyi
 * Maven工具
 */
public class MavenUtils {

    private static SnapshotVersion getSnapshotVersion(File xml){
        try {
            SAXReader reader = new SAXReader();
            Document document = reader.read(xml);
            Element root = document.getRootElement();
            Element versioning = root.element("versioning");
            long lastUpdated = Long.parseLong(versioning.elementText("lastUpdated"));
            for (Object version : versioning.element("snapshotVersions").elements("snapshotVersion")) {
                if (version instanceof Element){
                    if ("jar".equals(((Element) version).elementText("extension"))){
                        if (Long.parseLong(((Element) version).elementText("updated")) == lastUpdated){
                            String value = ((Element) version).elementText("value");
                            return new SnapshotVersion(lastUpdated,value);
                        }
                    }
                }
            }
        } catch (DocumentException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Optional<String> get(Project project,Dependency dependency){
        if (dependency.getVersion().toLowerCase().endsWith("-snapshot")){
            return getSnapshot(project,dependency);
        }else{
            return Optional.of(String.format(
                    "%s/%s/%s/%s/%s-%s.jar",
                    project.getLocalRepository(),
                    dependency.getGroupId().replaceAll("\\.", "/"),
                    dependency.getArtifactId(),
                    dependency.getVersion(),
                    dependency.getArtifactId(),
                    dependency.getVersion()
            ));
        }
    }

    private static String getLocation(String localRepository,Dependency dependency,SnapshotVersion version){
        return String.format(
                "%s/%s/%s/%s/%s-%s.jar",
                localRepository,
                dependency.getGroupId().replaceAll("\\.", "/"),
                dependency.getArtifactId(),
                dependency.getVersion(),
                dependency.getArtifactId(),
                version.getValue());
    }

    public static Optional<String> getSnapshot(Project project, Dependency dependency){
        String base = String.format(
                "%s/%s/%s/%s",
                project.getLocalRepository(),
                dependency.getGroupId().replaceAll("\\.", "/"),
                dependency.getArtifactId(),
                dependency.getVersion()
        );

        Optional<SnapshotVersion> version = Optional.ofNullable(new File(base).listFiles((file, name) -> name.startsWith("maven-metadata-") && name.endsWith(".xml")))
                .map(Arrays::stream)
                .orElseGet(Stream::empty)
                .map(MavenUtils::getSnapshotVersion)
                .filter(Objects::nonNull)
                .max(Comparator.comparing(SnapshotVersion::getLastUpdated));

        return version.map(v -> getLocation(project.getLocalRepository(),dependency,v));
    }

}
