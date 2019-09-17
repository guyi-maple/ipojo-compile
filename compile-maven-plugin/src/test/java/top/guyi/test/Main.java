package top.guyi.test;

import top.guyi.iot.ipojo.compile.lib.expand.compile.defaults.event.EventExpand;
import top.guyi.iot.ipojo.compile.lib.expand.compile.defaults.configuration.ConfigurationExpand;
import top.guyi.iot.ipojo.compile.lib.expand.compile.defaults.service.BundleServiceReferenceExpand;
import top.guyi.iot.ipojo.compile.lib.expand.compile.defaults.log.LoggerExpand;
import top.guyi.iot.ipojo.compile.lib.expand.compile.defaults.service.ServiceRegisterExpand;
import top.guyi.iot.ipojo.compile.lib.CompileExecutor;
import top.guyi.iot.ipojo.compile.lib.configuration.entry.Dependency;
import top.guyi.iot.ipojo.compile.lib.configuration.entry.Project;
import top.guyi.iot.ipojo.compile.lib.expand.manifest.defaults.*;

import java.util.HashSet;
import java.util.Set;

public class Main {

    public static void main(String[] args) throws Exception {
        Project project = new Project();
        project.setWork("/Users/guyi/Documents/Work/Java/ipojo-group/iot-gateway-api/iot-gateway-api-ulink/target/classes");
        project.setOutput("/Users/guyi/Documents/Work/Java/ipojo-group/iot-gateway-api/iot-gateway-api-ulink/target/compile");
        project.setGroupId("top.guyi.test");
        project.setArtifactId("ipojo-test");
        project.setVersion("1.0.0.0");
        project.setBaseDir("/Users/guyi/Documents/Work/Java/ipojo-group/ipojo-test/");
        project.setSourceDir("/Users/guyi/Documents/Work/Java/ipojo-group/ipojo-test/src/main");

        Set<Dependency> dependencies = new HashSet<>();
        dependencies.add(new Dependency(
                "/Users/guyi/.m2/repository",
                "top.guyi.iot.api",
                "iot-gateway-api-interface",
                "1.0.0.0",
                "compile"
        ));
        dependencies.add(new Dependency(
                "/Users/guyi/.m2/repository",
                "top.guyi.iot.protocol",
                "iot-gateway-protocol-ulink",
                "1.0.0.0",
                "compile"
        ));
        project.setDependencies(dependencies);

        CompileExecutor executor = new CompileExecutor();
        executor.compileExpand(new BundleServiceReferenceExpand());
        executor.compileExpand(new LoggerExpand());
        executor.compileExpand(new ServiceRegisterExpand());
        executor.compileExpand(new EventExpand());
        executor.compileExpand(new ConfigurationExpand());

        executor.manifestExpand(new BaseManifestExpand());
        executor.manifestExpand(new ActivatorManifestExpand());
        executor.manifestExpand(new DependencyManifestExpand());
        executor.manifestExpand(new TemplateManifestExpand());
        executor.manifestExpand(new ExportManifestExpand());

        executor.execute(project);
    }

}
