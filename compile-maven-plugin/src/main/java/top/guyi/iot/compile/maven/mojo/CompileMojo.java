package top.guyi.iot.compile.maven.mojo;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import top.guyi.iot.ipojo.compile.expand.component.DependencyComponentExpand;
import top.guyi.iot.ipojo.compile.expand.manifest.*;
import top.guyi.iot.ipojo.compile.expand.service.BundleServiceReferenceExpand;
import top.guyi.iot.ipojo.compile.expand.service.LoggerExpand;
import top.guyi.iot.ipojo.compile.expand.service.ServiceRegisterExpand;
import top.guyi.iot.ipojo.compile.lib.compile.CompileExecutor;
import top.guyi.iot.ipojo.compile.lib.project.entry.Dependency;
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
import top.guyi.iot.ipojo.compile.lib.project.configuration.ProjectInfo;

import java.util.*;
import java.util.stream.Collectors;

@Mojo(name = "compile",defaultPhase = LifecyclePhase.COMPILE)
public class CompileMojo extends AbstractMojo {

    @Parameter(property = "project")
    private MavenProject project;
    @Parameter(property = "session")
    private MavenSession session;
    @Component
    private DependencyGraphBuilder builder;

    @Override
    public void execute() {
        try {
            CompileExecutor executor = new CompileExecutor();

            executor.compileExpand(new BundleServiceReferenceExpand());
            executor.compileExpand(new LoggerExpand());
            executor.compileExpand(new DependencyComponentExpand());
            executor.compileExpand(new ServiceRegisterExpand());

            executor.manifestExpand(new BaseManifestExpand());
            executor.manifestExpand(new ActivatorManifestExpand());
            executor.manifestExpand(new DependencyManifestExpand());
            executor.manifestExpand(new TemplateManifestExpand());
            executor.manifestExpand(new ExportManifestExpand());

            executor.execute(project.getBuild().getOutputDirectory(),this.createProjectInfo());

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    private ProjectInfo createProjectInfo() throws DependencyGraphBuilderException {
        ProjectInfo projectInfo = new ProjectInfo();
        projectInfo.setGroupId(this.project.getGroupId());
        projectInfo.setArtifactId(this.project.getArtifactId());
        projectInfo.setVersion(this.project.getVersion());
        projectInfo.setDependencies(this.getDependency());
        projectInfo.setBaseDir(this.project.getBasedir().getAbsolutePath());
        return projectInfo;
    }

    private Set<Dependency> getDependency() throws DependencyGraphBuilderException {
        ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
        buildingRequest.setProject(project);

        String repository = session.getRepositorySession().getLocalRepository().getBasedir().getAbsolutePath();

        Set<DependencyNode> dependencyNodes = new HashSet<>();
        listDependencyNode(builder.buildDependencyGraph(buildingRequest, null),dependencyNodes);

        return dependencyNodes
                .stream()
                .map(DependencyNode::getArtifact)
                .filter(artifact -> !(artifact.getArtifactId().equals(project.getArtifactId())
                        && artifact.getGroupId().equals(project.getGroupId())))
                .map(artifact -> new Dependency(
                        repository,
                        artifact.getGroupId(),
                        artifact.getArtifactId(),
                        artifact.getVersion(),
                        artifact.getScope()))
                .collect(Collectors.toSet());
    }

    private Set<DependencyNode> listDependencyNode(DependencyNode node, Set<DependencyNode> nodes){
        nodes.add(node);
        Optional.ofNullable(node.getChildren())
                .ifPresent(children -> children.forEach(child -> listDependencyNode(child,nodes)));
        return nodes;
    }

}
