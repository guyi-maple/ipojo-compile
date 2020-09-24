package tech.guyi.ipojo.compile.maven.mojo.utils;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.eclipse.aether.repository.RemoteRepository;
import tech.guyi.ipojo.compile.lib.configuration.entry.Dependency;
import tech.guyi.ipojo.compile.lib.configuration.entry.Project;
import tech.guyi.ipojo.compile.lib.configuration.entry.Repository;
import tech.guyi.ipojo.compile.lib.configuration.entry.Server;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ProjectUtils {

    public static Project createProjectInfo(MavenSession session, MavenProject project, List<RemoteRepository> remoteRepos,DependencyGraphBuilder builder) throws DependencyGraphBuilderException {
        Project info = new Project();
        info.setServers(
                session.getRequest().getServers()
                        .stream()
                        .map(server -> new Server(server.getId(),server.getUsername(),server.getPassword()))
                        .collect(Collectors.toSet())
        );
        info.setGroupId(project.getGroupId());
        info.setArtifactId(project.getArtifactId());
        info.setVersion(project.getVersion());
        info.setDependencies(getDependency(session,project,builder));
        info.setBaseDir(project.getBasedir().getAbsolutePath());
        info.setWork(project.getBuild().getOutputDirectory());
        info.setLocalRepository(session.getRepositorySession().getLocalRepository().getBasedir().getAbsolutePath());
        info.setRepositories(
                remoteRepos
                        .stream()
                        .map(repo -> new Repository(
                                repo.getId(),
                                repo.getContentType(),
                                repo.getUrl())
                        ).collect(Collectors.toList())
        );
        return info;
    }

    private static Set<Dependency> getDependency(MavenSession session, MavenProject project, DependencyGraphBuilder builder) throws DependencyGraphBuilderException {
        ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
        buildingRequest.setProject(project);

        Set<DependencyNode> dependencyNodes = new HashSet<>();
        listDependencyNode(builder.buildDependencyGraph(buildingRequest, null),dependencyNodes);

        return dependencyNodes
                .stream()
                .map(DependencyNode::getArtifact)
                .filter(artifact -> !(artifact.getArtifactId().equals(project.getArtifactId())
                        && artifact.getGroupId().equals(project.getGroupId())))
                .filter(artifact -> !"scope".equals(artifact.getScope()))
                .map(artifact -> new Dependency(
                        artifact.getGroupId(),
                        artifact.getArtifactId(),
                        artifact.getBaseVersion(),
                        artifact.getScope()))
                .collect(Collectors.toSet());
    }

    private static Set<DependencyNode> listDependencyNode(DependencyNode node, Set<DependencyNode> nodes){
        nodes.add(node);
        Optional.ofNullable(node.getChildren())
                .ifPresent(children -> children.forEach(child -> listDependencyNode(child,nodes)));
        return nodes;
    }

}
