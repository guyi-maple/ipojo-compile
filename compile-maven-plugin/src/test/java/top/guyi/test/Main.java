package top.guyi.test;

import top.guyi.iot.ipojo.compile.expand.component.DependencyComponentExpand;
import top.guyi.iot.ipojo.compile.expand.component.EventExpand;
import top.guyi.iot.ipojo.compile.expand.configuration.ConfigurationExpand;
import top.guyi.iot.ipojo.compile.expand.manifest.*;
import top.guyi.iot.ipojo.compile.expand.service.BundleServiceReferenceExpand;
import top.guyi.iot.ipojo.compile.expand.service.LoggerExpand;
import top.guyi.iot.ipojo.compile.expand.service.ServiceRegisterExpand;
import top.guyi.iot.ipojo.compile.lib.compile.CompileExecutor;
import top.guyi.iot.ipojo.compile.lib.configuration.entry.Project;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {

    public static void main(String[] args) throws Exception {
        Project project = new Project();
        project.setWork("/Users/guyi/Documents/Work/Java/ipojo-group/ipojo-test/target/classes");
        project.setOutput("/Users/guyi/Documents/Work/Java/ipojo-group/ipojo-test/target/compile");
        project.setGroupId("top.guyi.test");
        project.setArtifactId("ipojo-test");
        project.setVersion("1.0.0.0");
        project.setBaseDir("/Users/guyi/Documents/Work/Java/ipojo-group/ipojo-test/");
        project.setSourceDir("/Users/guyi/Documents/Work/Java/ipojo-group/ipojo-test/src/main");

        CompileExecutor executor = new CompileExecutor();
        executor.compileExpand(new BundleServiceReferenceExpand());
        executor.compileExpand(new LoggerExpand());
        executor.compileExpand(new DependencyComponentExpand());
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
