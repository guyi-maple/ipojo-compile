package top.guyi.iot.compile.maven.mojo.configuration;

import lombok.Data;
import org.apache.maven.project.MavenProject;
import top.guyi.iot.ipojo.compile.lib.utils.StringUtils;
import top.guyi.iot.ipojo.compile.lib.configuration.entry.Dependency;
import top.guyi.iot.ipojo.compile.lib.configuration.entry.Repository;
import top.guyi.iot.ipojo.compile.lib.configuration.entry.Server;
import top.guyi.iot.ipojo.compile.lib.maven.MavenHelper;
import top.guyi.iot.ipojo.compile.lib.utils.MavenUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

@Data
public class FelixConfiguration {

    private MavenProject project;
    private String localRepository;
    private String projectBundle;
    private Set<Server> servers = Collections.emptySet();
    private List<Repository> repositories;

    private Map<String,String> config;
    private List<Dependency> bundles;

    public FelixConfiguration() {
        this.config = new HashMap<>();
        config.put("org.osgi.framework.storage.clean","onFirstInit");
        config.put("felix.auto.deploy.action","install,start");

        this.bundles = new LinkedList<>();
        bundles.add(new Dependency("org.fusesource.jansi","jansi","1.17.1",null));
        bundles.add(new Dependency("org.jline","jline","3.7.0",null));
        bundles.add(new Dependency("org.apache.felix","org.apache.felix.eventadmin","1.5.0",null));
        bundles.add(new Dependency("org.apache.felix","org.apache.felix.log","1.2.0",null));
        bundles.add(new Dependency("org.apache.felix","org.apache.felix.gogo.runtime","1.1.0",null));
        bundles.add(new Dependency("org.apache.felix","org.apache.felix.gogo.command","1.0.2",null));
        bundles.add(new Dependency("org.apache.felix","org.apache.felix.gogo.jline","1.1.0",null));
    }

    public void setConfig(Map<String, String> config) {
        if (!config.containsKey("org.osgi.framework.storage.clean")){
            config.put("org.osgi.framework.storage.clean","onFirstInit");
        }
        if (!config.containsKey("felix.auto.deploy.action")){
            config.put("felix.auto.deploy.action","install,start");
        }
        this.config = config;
    }

    public void setBundles(List<Dependency> bundles) {
        if (bundles.stream().noneMatch(dependency -> "jansi".equals(dependency.getArtifactId()))){
            bundles.add(new Dependency("org.fusesource.jansi","jansi","1.17.1",null));
        }
        if (bundles.stream().noneMatch(dependency -> "jline".equals(dependency.getArtifactId()))){
            bundles.add(new Dependency("org.jline","jline","3.7.0",null));
        }
        if (bundles.stream().noneMatch(dependency -> "org.apache.felix.eventadmin".equals(dependency.getArtifactId()))){
            bundles.add(new Dependency("org.apache.felix","org.apache.felix.eventadmin","1.5.0",null));
        }
        if (bundles.stream().noneMatch(dependency -> "org.apache.felix.log".equals(dependency.getArtifactId()))){
            bundles.add(new Dependency("org.apache.felix","org.apache.felix.log","1.2.0",null));
        }
        if (bundles.stream().noneMatch(dependency -> "org.apache.felix.gogo.runtime".equals(dependency.getArtifactId()))){
            bundles.add(new Dependency("org.apache.felix","org.apache.felix.gogo.runtime","1.1.0",null));
        }
        if (bundles.stream().noneMatch(dependency -> "org.apache.felix.gogo.command".equals(dependency.getArtifactId()))){
            bundles.add(new Dependency("org.apache.felix","org.apache.felix.gogo.command","1.0.2",null));
        }
        if (bundles.stream().noneMatch(dependency -> "org.apache.felix.gogo.jline".equals(dependency.getArtifactId()))){
            bundles.add(new Dependency("org.apache.felix","org.apache.felix.gogo.jline","1.1.0",null));
        }
        this.bundles = bundles;
    }

    public String getProjectBundle() {
        if (StringUtils.isEmpty(this.projectBundle)){
            File target = new File(String.format("%s/target",project.getBasedir()));
            String artifactId = project.getArtifactId();
            this.projectBundle = Optional.ofNullable(target.listFiles((dir, name) -> name.contains(artifactId) && name.endsWith(".jar")))
                    .map(Arrays::stream)
                    .flatMap(Stream::findFirst)
                    .map(file -> String.format("file:///%s",file.getAbsolutePath()))
                    .orElse(null);
        }

        if (this.projectBundle != null && !this.projectBundle.startsWith("file:///")){
            this.projectBundle = String.format("file:///%s/%s",project.getBasedir(),this.projectBundle);
        }

        return this.projectBundle;
    }

    public Map<String,String> getConfigMap(){
        Map<String,String> config = this.getConfig();
        if (!config.containsKey("felix.auto.start.1")){
            StringBuilder sb = new StringBuilder();
            this.getBundles().forEach(bundle -> {
                MavenUtils.get(this.localRepository,bundle)
                        .ifPresent(path -> {
                            if (Files.notExists(Paths.get(path))){
                                MavenHelper.resolveArtifact(
                                        this.repositories,
                                        this.servers,
                                        this.localRepository,
                                        bundle.getName()
                                );
                            }
                            sb.append("file:///").append(path).append(" ");
                        });
            });
            if (!StringUtils.isEmpty(this.getProjectBundle())){
                sb.append(this.getProjectBundle());
            }
            config.put("felix.auto.start.1",sb.toString());
        }
        return config;
    }

}