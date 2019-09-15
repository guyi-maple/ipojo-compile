package top.guyi.test;

import top.guyi.iot.ipojo.compile.expand.component.DependencyComponentExpand;
import top.guyi.iot.ipojo.compile.expand.component.EventExpand;
import top.guyi.iot.ipojo.compile.expand.manifest.*;
import top.guyi.iot.ipojo.compile.expand.service.BundleServiceReferenceExpand;
import top.guyi.iot.ipojo.compile.expand.service.LoggerExpand;
import top.guyi.iot.ipojo.compile.expand.service.ServiceRegisterExpand;
import top.guyi.iot.ipojo.compile.lib.compile.CompileExecutor;
import top.guyi.iot.ipojo.compile.lib.configuration.Compile;
import top.guyi.iot.ipojo.compile.lib.configuration.entry.Dependency;
import top.guyi.iot.ipojo.compile.lib.configuration.entry.Project;

import java.util.Collections;

public class Main {

    public static void main(String[] args) throws Exception {
        Project project = new Project();
        project.setName("ipojo-test");
        project.setArtifactId(project.getName());
        project.setVersion("1.0.0.0");
        project.setGroupId("top.guyi.test");
        project.setBaseDir("/Users/guyi/Documents/Work/Java/ipojo-group/ipojo-test");
        project.setWork("/Users/guyi/Documents/Work/Java/ipojo-group/ipojo-test/target/classes");
        project.setOutput(project.getWork() + "/compile");
        Dependency dependency = new Dependency(
                "/Users/guyi/.m2/repository",
                "top.guyi.iot.ipojo",
                "ipojo",
                "1.3.0.0",
                "compile"
        );
        project.setDependencies(Collections.singleton(dependency));

        CompileExecutor executor = new CompileExecutor();
        executor.compileExpand(new BundleServiceReferenceExpand());
        executor.compileExpand(new LoggerExpand());
        executor.compileExpand(new DependencyComponentExpand());
        executor.compileExpand(new ServiceRegisterExpand());
        executor.compileExpand(new EventExpand());
        executor.manifestExpand(new BaseManifestExpand());
        executor.manifestExpand(new ActivatorManifestExpand());
        executor.manifestExpand(new DependencyManifestExpand());
        executor.manifestExpand(new TemplateManifestExpand());
        executor.manifestExpand(new ExportManifestExpand());

        Compile compile = executor.execute(project);
        System.out.println(compile);
    }

}
