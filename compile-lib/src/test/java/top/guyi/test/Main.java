package top.guyi.test;

import top.guyi.iot.ipojo.compile.lib.CompileExecutor;
import top.guyi.iot.ipojo.compile.lib.configuration.entry.Dependency;
import top.guyi.iot.ipojo.compile.lib.configuration.entry.Project;

import java.util.*;

public class Main {

    public static void main(String[] args) throws Exception {

        CompileExecutor executor = new CompileExecutor();

        Project project = new Project();
        project.setWork("/Users/guyi/Documents/Work/Java/iot-1.3-group/ipojo-test/target/classes");
        project.setOutput("/Users/guyi/Documents/Work/Java/iot-1.3-group/ipojo-test/target/compile");
        project.setLocalRepository("/Users/guyi/.m2/repository");
        Dependency dependency = new Dependency(
                "top.guyi.iot.ipojo",
                "ipojo",
                "1.0.0.0",
                "compile"
        );
        Set<Dependency> dependencies = new HashSet<>();
        dependencies.add(dependency);
        project.setDependencies(dependencies);

        executor.execute(project);


    }

}
