package top.guyi.iot.ipojo.compile.expand.helper;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class ImportDependencyExclude {

    private static List<String> dependencyExclude = Arrays.asList(
            "org.eclipse.sisu.plexus",
            "plexus-classworlds",
            "xbean-reflect",
            "slf4j-api",
            "maven-resolver",
            "org.eclipse.sisu.inject",
            "google/inject/guice",
            "google/guava/guava",
            "aether-util",
            "idea_rt",
            "jre/lib/",
            "maven-plugin-api",
            "maven-model",
            "maven-artifact",
            "enterprise/cdi-api",
            "annotation/jsr250-api",
            "plexus/plexus-utils",
            "plugin-tools/maven-plugin-annotations"
    );

    public static boolean matchDependency(String classPath){
        return dependencyExclude.stream().noneMatch(classPath::contains);
    }

}
