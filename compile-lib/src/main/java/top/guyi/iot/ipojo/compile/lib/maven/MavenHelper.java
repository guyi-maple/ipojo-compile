package top.guyi.iot.ipojo.compile.lib.maven;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
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
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import top.guyi.iot.ipojo.compile.lib.configuration.entry.Project;
import top.guyi.iot.ipojo.compile.lib.configuration.entry.Repository;
import top.guyi.iot.ipojo.compile.lib.configuration.entry.Server;
import top.guyi.iot.ipojo.compile.lib.maven.util.Booter;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class MavenHelper {

    private static RepositorySystemSession buildSession(top.guyi.iot.ipojo.compile.lib.configuration.entry.Dependency root,RepositorySystem system,Project project){
        return root.get(project)
                .filter(path -> Files.exists(Paths.get(path)))
                .map(p -> {
                    DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
                    LocalRepository localRepo = new LocalRepository( project.getLocalRepository());
                    session.setLocalRepositoryManager( system.newLocalRepositoryManager( session, localRepo ));
                    return session;
                })
                .orElseGet(() -> Booter.newRepositorySystemSession(system,project.getLocalRepository()));
    }

    public static Set<top.guyi.iot.ipojo.compile.lib.configuration.entry.Dependency> getDependencies(
            Project project,
            top.guyi.iot.ipojo.compile.lib.configuration.entry.Dependency root){
        try {
            RepositorySystem system = Booter.newRepositorySystem();
            RepositorySystemSession session = buildSession(root,system,project);
            CollectRequest request = buildRequest(root,project.getRepositories(),project.getServers());
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
                    .filter(dependency -> dependency.get(project)
                            .map(path -> Files.notExists(Paths.get(path)))
                            .orElse(false))
                    .forEach(dependency -> resolveArtifact(request,system,session,dependency.getName()));

            return dependencies;
        } catch (DependencyCollectionException e) {
            e.printStackTrace();
        }
        return Collections.singleton(root);
    }

    public static void resolveArtifact(CollectRequest request,RepositorySystem system,RepositorySystemSession session,String coords){
        try {
            Artifact tmp = new DefaultArtifact(coords);
            ArtifactRequest artifactRequest = new ArtifactRequest();
            artifactRequest.setRepositories(request.getRepositories());
            artifactRequest.setArtifact(tmp);
            system.resolveArtifact(session,artifactRequest);
        } catch (ArtifactResolutionException e) {
            e.printStackTrace();
        }
    }

    public static void resolveArtifact(CollectRequest request,String localRepository,String coords){
        RepositorySystem system = Booter.newRepositorySystem();
        RepositorySystemSession session = Booter.newRepositorySystemSession(system,localRepository);
        resolveArtifact(request,system,session,coords);
    }

    public static void resolveArtifact(List<Repository> repositories,Set<Server> servers,String localRepository, String coords){
        resolveArtifact(buildRequest(coords,"",repositories,servers),localRepository,coords);
    }

    private static CollectRequest buildRequest(
            String coords,
            String scope,
            List<Repository> repositories,
            Set<Server> servers){
        Artifact artifact = new DefaultArtifact(coords);
        CollectRequest request  = new CollectRequest();
        request.setRoot(new Dependency(artifact,scope));

        if (repositories == null || repositories.isEmpty()){
            request.setRepositories(getDefaultRepository());
        }else{
            request.setRepositories(
                    repositories
                            .stream()
                            .map(repo -> {
                                RemoteRepository.Builder builder = new RemoteRepository.Builder(
                                        repo.getId(),
                                        repo.getType(),
                                        repo.getUrl());
                                servers.stream()
                                        .filter(server -> server.getId().equals(repo.getId()))
                                        .findFirst()
                                        .ifPresent(server -> builder.setAuthentication(
                                                new AuthenticationBuilder()
                                                        .addUsername(server.getUsername())
                                                        .addPassword(server.getPassword())
                                                        .build()
                                                )
                                        );
                                return builder.build();
                            }).collect(Collectors.toList())
            );
        }

        return request;
    }

    private static CollectRequest buildRequest(
            top.guyi.iot.ipojo.compile.lib.configuration.entry.Dependency root,
            List<Repository> repositories,
            Set<Server> servers){
        return buildRequest(root.getName(),root.getScope(),repositories,servers);
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
