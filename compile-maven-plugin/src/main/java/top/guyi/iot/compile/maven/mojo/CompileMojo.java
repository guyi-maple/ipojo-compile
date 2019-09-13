package top.guyi.iot.compile.maven.mojo;

import top.guyi.iot.ipojo.compile.expand.service.BundleServiceReferenceExpand;
import top.guyi.iot.ipojo.compile.expand.service.LoggerExpand;
import top.guyi.iot.ipojo.compile.lib.compile.CompileExecutor;
import top.guyi.iot.ipojo.compile.lib.compile.entry.Dependency;
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

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Mojo(name = "ipojo-compile")
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
            List<Dependency> dependencies = this.getDependency();

            CompileExecutor executor = new CompileExecutor();
            executor.compileInfoSetter(info -> {
                dependencies.forEach(info::addDependency);
                return info;
            });

            executor.compileExpand(new BundleServiceReferenceExpand());
            executor.compileExpand(new LoggerExpand());

            executor.execute(project.getBuild().getOutputDirectory());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    private List<Dependency> getDependency() throws DependencyGraphBuilderException {
        ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
        buildingRequest.setProject(project);

        String repository = session.getRepositorySession().getLocalRepository().getBasedir().getAbsolutePath();

        List<DependencyNode> dependencyNodes = new LinkedList<>();
        listDependencyNode(builder.buildDependencyGraph(buildingRequest, null),dependencyNodes);

        return dependencyNodes
                .stream()
                .map(DependencyNode::getArtifact)
                .filter(artifact -> !artifact.getArtifactId().equals(project.getArtifactId())
                        && !artifact.getGroupId().equals(project.getGroupId()))
                .map(artifact -> new Dependency(
                        repository,
                        artifact.getGroupId(),
                        artifact.getArtifactId(),
                        artifact.getVersion(),
                        artifact.getScope()))
                .collect(Collectors.toList());
    }

    private List<DependencyNode> listDependencyNode(DependencyNode node, List<DependencyNode> nodes){
        nodes.add(node);
        Optional.ofNullable(node.getChildren())
                .ifPresent(children -> children.forEach(child -> listDependencyNode(child,nodes)));
        return nodes;
    }

}
