package top.guyi.iot.compile.maven.mojo;

import com.google.gson.Gson;
import lombok.SneakyThrows;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.repository.RemoteRepository;
import top.guyi.iot.compile.maven.mojo.configuration.FelixConfiguration;
import top.guyi.iot.ipojo.compile.lib.configuration.entry.Repository;
import top.guyi.iot.ipojo.compile.lib.configuration.entry.Server;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

@Mojo(name = "felix")
public class FelixMojo extends AbstractMojo {

    @Parameter(property = "project")
    private MavenProject project;
    @Parameter(property = "session")
    private MavenSession session;
    @Parameter(property = "project.remoteProjectRepositories")
    private List<RemoteRepository> remoteRepos;

    @Override
    @SneakyThrows
    public void execute() {
        String base = project.getBasedir().getAbsolutePath();
        this.startFelix(base);
    }

    private void startFelix(String base) throws Exception {
        Gson gson = new Gson();
        File json = new File(String.format("%s/configuration.felix",base));
        FelixConfiguration configuration;
        if (json.exists()){
            configuration = gson.fromJson(
                    new FileReader(json),
                    FelixConfiguration.class
            );
            configuration.init();
        }else{
            configuration = new FelixConfiguration();
            configuration.setConfig(new HashMap<>());
            configuration.setBundles(new LinkedList<>());
        }
        this.setConfigurationAttach(configuration);
        this.writeConfig(base,configuration);
        org.apache.felix.main.Main.main(new String[0]);
    }

    private void setConfigurationAttach(FelixConfiguration configuration){
        configuration.setProject(this.project);
        configuration.setRepositories(
                this.remoteRepos
                        .stream()
                        .map(repo -> new Repository(
                                repo.getId(),
                                repo.getContentType(),
                                repo.getUrl())
                        ).collect(Collectors.toList())
        );
        configuration.setServers(
                session.getRequest().getServers()
                        .stream()
                        .map(server -> new Server(server.getId(),server.getUsername(),server.getPassword()))
                        .collect(Collectors.toSet())
        );
        configuration.setLocalRepository(session.getRepositorySession().getLocalRepository().getBasedir().getAbsolutePath());
    }

    private void writeConfig(String base,FelixConfiguration configuration) throws IOException {
        File configDir = new File(String.format("%s/conf",base));
        if (!configDir.exists()){
            configDir.mkdirs();
        }

        Properties properties = new Properties();
        configuration.getConfigMap().forEach(properties::setProperty);
        properties.store(new FileOutputStream(String.format("%s/conf/config.properties",base)),null);
    }
}
