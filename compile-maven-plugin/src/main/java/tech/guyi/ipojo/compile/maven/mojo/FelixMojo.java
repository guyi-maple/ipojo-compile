package tech.guyi.ipojo.compile.maven.mojo;

import com.google.gson.Gson;
import lombok.SneakyThrows;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.eclipse.aether.repository.RemoteRepository;
import tech.guyi.ipojo.compile.maven.mojo.configuration.FelixConfiguration;
import tech.guyi.ipojo.compile.maven.mojo.utils.ProjectUtils;

import java.io.*;
import java.util.*;

@Mojo(name = "felix")
public class FelixMojo extends AbstractMojo {

    @Parameter(property = "project")
    private MavenProject project;
    @Parameter(property = "session")
    private MavenSession session;
    @Parameter(property = "project.remoteProjectRepositories")
    private List<RemoteRepository> remoteRepos;
    @Component
    private DependencyGraphBuilder builder;

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
        configuration.setProject(ProjectUtils.createProjectInfo(session,project,remoteRepos,builder));
        this.writeConfig(base,configuration);
        org.apache.felix.main.Main.main(configuration.getArgs().toArray(new String[0]));
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
