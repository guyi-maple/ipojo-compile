package top.guyi.iot.compile.maven.mojo;

import com.google.gson.Gson;
import lombok.SneakyThrows;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import top.guyi.iot.compile.maven.mojo.configuration.FelixConfiguration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.*;

@Mojo(name = "felix", defaultPhase = LifecyclePhase.TEST)
public class FelixMojo extends AbstractMojo {

    @Parameter(property = "project")
    private MavenProject project;

    @Override
    @SneakyThrows
    public void execute() {
        String base = project.getBasedir().getAbsolutePath();
        this.startFelix(base);
    }

    private void setBundle(String base, List<String> bundles){
        File target = new File(String.format("%s/target",base));
        String artifactId = project.getArtifactId();
        Optional.ofNullable(target.listFiles((dir, name) -> name.contains(artifactId) && name.endsWith(".jar")))
                .ifPresent(files -> Arrays.stream(files).forEach(file -> bundles.add(String.format("file:///%s",file.getAbsolutePath()))));
    }

    private void startFelix(String base) throws Exception {
        Gson gson = new Gson();

        File json = new File(String.format("%s/felix.config.json",base));
        FelixConfiguration configuration;
        if (json.exists()){
            configuration = gson.fromJson(
                    new FileReader(json),
                    FelixConfiguration.class
            );
        }else{
            configuration = new FelixConfiguration();
        }

        List<String> bundles = new LinkedList<>();
        bundles.add("https://maven.aliyun.com/repository/public/org/fusesource/jansi/jansi/1.17.1/jansi-1.17.1.jar");
        bundles.add("https://maven.aliyun.com/repository/public/org/jline/jline/3.7.0/jline-3.7.0.jar");
        bundles.add("https://maven.aliyun.com/repository/public/org/apache/felix/org.apache.felix.eventadmin/1.5.0/org.apache.felix.eventadmin-1.5.0.jar");
        bundles.add("https://maven.aliyun.com/repository/public/org/apache/felix/org.apache.felix.log/1.2.0/org.apache.felix.log-1.2.0.jar");
        bundles.add("https://maven.aliyun.com/repository/public/org/apache/felix/org.apache.felix.gogo.runtime/1.1.0/org.apache.felix.gogo.runtime-1.1.0.jar");
        bundles.add("https://maven.aliyun.com/repository/public/org/apache/felix/org.apache.felix.gogo.command/1.0.2/org.apache.felix.gogo.command-1.0.2.jar");
        bundles.add("https://maven.aliyun.com/repository/public/org/apache/felix/org.apache.felix.gogo.jline/1.1.0/org.apache.felix.gogo.jline-1.1.0.jar");
        configuration.addDefaultBundles(bundles);

        this.setBundle(base,configuration.getBundles());

        File configDir = new File(String.format("%s/conf",base));
        if (!configDir.exists()){
            configDir.mkdirs();
        }
        Properties properties = new Properties();
        configuration.toMap().forEach(properties::setProperty);
        properties.store(new FileOutputStream(String.format("%s/config.properties",configDir.getAbsoluteFile())),null);
        org.apache.felix.main.Main.main(new String[0]);
    }

}
