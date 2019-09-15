package top.guyi.iot.ipojo.compile.lib.configuration.entry;

import lombok.Data;
import top.guyi.iot.ipojo.application.utils.StringUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Data
public class Project {

    private String baseDir;
    private String sourceDir;
    private String work;
    private String output;

    private String name;
    private String symbolicName;
    private String finalName;
    private String version;
    private String groupId;
    private String artifactId;

    private Set<Dependency> dependencies = Collections.emptySet();

    public void setWork(String work){
        work = work.replace("\\","/");
        work = work.endsWith("/") ? work : work + "/";
        this.work = work;
    }

    public String getOutput(){
        return Optional.ofNullable(this.output).orElse(this.work);
    }

    public String getSymbolicName(){
        return Optional.ofNullable(this.symbolicName).orElse(this.name);
    }

    public void extend(Project project){
        this.work = project.work;
        this.baseDir = project.baseDir;
        this.sourceDir = project.sourceDir;

        if (StringUtils.isEmpty(name)){
            this.name = project.name;
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

        if (project.getDependencies() != null && project.getDependencies().size() > 0){
            Set<Dependency> dependencies = new HashSet<>(this.dependencies);
            dependencies.addAll(project.dependencies);
            this.dependencies = dependencies;
        }
    }

}
