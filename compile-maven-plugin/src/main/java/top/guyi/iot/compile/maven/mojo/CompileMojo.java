package top.guyi.iot.compile.maven.mojo;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.eclipse.aether.repository.RemoteRepository;
import top.guyi.iot.compile.maven.mojo.utils.ProjectUtils;
import top.guyi.iot.ipojo.compile.lib.CompileExecutor;
import top.guyi.iot.ipojo.compile.lib.configuration.entry.Dependency;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import top.guyi.iot.ipojo.compile.lib.configuration.entry.Project;
import top.guyi.iot.ipojo.compile.lib.configuration.entry.Repository;
import top.guyi.iot.ipojo.compile.lib.configuration.entry.Server;

import java.util.*;
import java.util.stream.Collectors;

@Mojo(name = "compile",defaultPhase = LifecyclePhase.COMPILE)
public class CompileMojo extends AbstractMojo {

    @Parameter(property = "project")
    private MavenProject project;
    @Parameter(property = "session")
    private MavenSession session;
    @Parameter(property = "project.remoteProjectRepositories")
    private List<RemoteRepository> remoteRepos;
    @Component
    private DependencyGraphBuilder builder;

    @Override
    public void execute() {
        try {
            CompileExecutor executor = new CompileExecutor();
            executor.execute(ProjectUtils.createProjectInfo(session,project,remoteRepos,builder))
                    .ifPresent(compile -> this.project.getBuild().setFinalName(
                            Optional.ofNullable(compile.getProject().getFinalName())
                                    .map(name -> name + "-" + compile.getProject().getVersion())
                                    .orElse(String.format("%s-%s",this.project.getArtifactId(),compile.getProject().getVersion()))
                    ));
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

}
