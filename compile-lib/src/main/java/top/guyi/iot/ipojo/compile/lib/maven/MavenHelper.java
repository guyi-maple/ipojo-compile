package top.guyi.iot.ipojo.compile.lib.maven;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.transfer.ArtifactNotFoundException;
import top.guyi.iot.ipojo.compile.lib.configuration.entry.Project;
import top.guyi.iot.ipojo.compile.lib.maven.util.Booter;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

public class MavenHelper {

    public static void main(String[] args) throws DependencyCollectionException {
        getDependencies(new Project(),new top.guyi.iot.ipojo.compile.lib.configuration.entry.Dependency(
                "com.robotaiot.iot.api.gateway",
                "iot-gateway-api-andlink",
                "1.0.0.0",
                "compile"
        )).forEach(System.out::println);
    }


    public static Set<top.guyi.iot.ipojo.compile.lib.configuration.entry.Dependency> getDependencies(Project project,top.guyi.iot.ipojo.compile.lib.configuration.entry.Dependency root){
        try {
            RepositorySystem system = Booter.newRepositorySystem();
            RepositorySystemSession session = Booter.newRepositorySystemSession(system,project);
            Artifact artifact = new DefaultArtifact(root.getName());
            CollectRequest request  = new CollectRequest();
            request.setRoot(new Dependency(artifact,root.getScope()));

            if (project.getRepositories().isEmpty()){
                request.setRepositories(getDefaultRepository());
            }else{
                request.setRepositories(
                        project.getRepositories()
                                .stream()
                                .map(repo -> new RemoteRepository.Builder(
                                        repo.getId(),
                                        repo.getType(),
                                        repo.getUrl())
                                        .build()
                                ).collect(Collectors.toList())
                );
            }

            CollectResult result = system.collectDependencies( session, request );
            Set<top.guyi.iot.ipojo.compile.lib.configuration.entry.Dependency> dependencies = new HashSet<>();
            result.getRoot().accept(new DependencyVisitor() {
                @Override
                public boolean visitEnter(DependencyNode node) {
                    dependencies.add(
                            new top.guyi.iot.ipojo.compile.lib.configuration.entry.Dependency(
                                    node.getDependency().getArtifact().getGroupId(),
                                    node.getDependency().getArtifact().getArtifactId(),
                                    node.getDependency().getArtifact().getVersion(),
                                    node.getDependency().getScope()
                            )
                    );
                    return true;
                }
                @Override
                public boolean visitLeave(DependencyNode dependencyNode) {
                    return true;
                }
            });
            dependencies.add(root);

            dependencies
                    .stream()
                    .filter(dependency -> {
                        try {
                            return !new File(dependency.getURL(project).toURI()).exists();
                        } catch (MalformedURLException | URISyntaxException e) {
                            e.printStackTrace();
                            return false;
                        }
                    })
                    .forEach(dependency -> {
                        try {
                            Artifact tmp = new DefaultArtifact(dependency.getName());
                            ArtifactRequest artifactRequest = new ArtifactRequest();
                            artifactRequest.setRepositories(request.getRepositories());
                            artifactRequest.setArtifact(tmp);
                            system.resolveArtifact(session,artifactRequest);
                        } catch (ArtifactResolutionException e) {
                            e.printStackTrace();
                        }
                    });

            return dependencies;
        } catch (DependencyCollectionException e) {
            e.printStackTrace();
        }
        return Collections.singleton(root);
    }

    private static List<RemoteRepository> getDefaultRepository(){
        return Arrays.asList(
                new RemoteRepository.Builder(
                        "aliyun",
                        "default",
                        "https://maven.aliyun.com/repository/public"
                ).build(),
                new RemoteRepository.Builder(
                        "guyi",
                        "default",
                        "http://nexus.guyi-maple.top/repository/maven-public/"
                ).build()
        );
    }

}
