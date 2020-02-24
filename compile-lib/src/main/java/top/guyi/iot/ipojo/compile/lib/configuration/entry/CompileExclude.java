package top.guyi.iot.ipojo.compile.lib.configuration.entry;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.Collections;
import java.util.Set;
import java.util.regex.Pattern;

@Data
public class CompileExclude {

    @Expose
    private Set<String> dependencyCopy = Collections.emptySet();

    @Expose
    @SerializedName("export")
    private Set<String> exportPackage = Collections.emptySet();

    @Expose
    @SerializedName("import")
    private Set<String> importPackage = Collections.emptySet();

    @Expose
    private Set<String> dependencyScope = Collections.emptySet();

    public boolean noneExport(String packageName){
        return this.exportPackage.stream()
                .noneMatch(name -> Pattern.matches(name,packageName));
    }

    public boolean noneImport(String packageName){
        return this.importPackage.stream()
                .noneMatch(name -> Pattern.matches(name,packageName));
    }

    public boolean noneDependencyCopy(Dependency dependency){
        String name = dependency.getName();
        return this.dependencyCopy.stream()
                .noneMatch(dependencyName -> Pattern.matches(dependencyName,name));
    }

    public boolean noneDependencyScope(Dependency dependency){
        return this.dependencyScope.stream()
                .noneMatch(scope -> Pattern.matches(scope,dependency.getScope()));
    }

}
