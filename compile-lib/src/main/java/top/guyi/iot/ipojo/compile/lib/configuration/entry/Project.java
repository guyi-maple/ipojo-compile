package top.guyi.iot.ipojo.compile.lib.configuration.entry;

import lombok.Data;
import top.guyi.iot.ipojo.application.utils.StringUtils;

import java.util.*;

@Data
public class Project {

    private String baseDir;
    private String sourceDir;
    private String work;
    private String output;

    private String finalName;
    private String version;
    private String groupId;
    private String artifactId;

    private String localRepository;
    private List<Repository> repositories = Collections.emptyList();

    private Set<Dependency> dependencies = Collections.emptySet();

    private Set<Server> servers = Collections.emptySet();

    public void setWork(String work){
        work = work.replace("\\","/");
        work = work.endsWith("/") ? work : work + "/";
        this.work = work;
    }

    public String getOutput(){
        return Optional.ofNullable(this.output).orElse(this.work);
    }

    public void extend(Project project){
        this.extend(project,true);
    }
    public void extend(Project project,boolean override){
        if (override){
            this.work = project.work;
            this.baseDir = project.getBaseDir();
            this.sourceDir = project.getSourceDir();
        }

        if (StringUtils.isEmpty(output)){
            this.output = project.output;
        }
        if (StringUtils.isEmpty(finalName)){
            this.finalName = project.finalName;
        }
        if (StringUtils.isEmpty(version)){
            this.version = project.version;
        }
        if (StringUtils.isEmpty(groupId)){
            this.groupId = project.groupId;
        }
        if (StringUtils.isEmpty(artifactId)){
            this.artifactId = project.artifactId;
        }
        if (StringUtils.isEmpty(localRepository)){
            this.localRepository = project.localRepository;
        }
        if (repositories.isEmpty()){
            this.repositories = project.repositories;
        }
        if (servers.isEmpty()){
            this.servers = project.servers;
        }
        if (project.getDependencies() != null && project.getDependencies().size() > 0){
            Set<Dependency> dependencies = new HashSet<>(this.dependencies);
            dependencies.addAll(project.dependencies);
            this.dependencies = dependencies;
        }
    }

}
