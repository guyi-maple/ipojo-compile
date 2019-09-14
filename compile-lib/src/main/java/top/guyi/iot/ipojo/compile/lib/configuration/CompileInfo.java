package top.guyi.iot.ipojo.compile.lib.configuration;

import com.google.gson.annotations.SerializedName;
import top.guyi.iot.ipojo.application.utils.StringUtils;
import top.guyi.iot.ipojo.compile.lib.compile.exception.CompileInfoCheckException;
import lombok.Data;
import top.guyi.iot.ipojo.compile.lib.enums.CompileType;
import top.guyi.iot.ipojo.compile.lib.project.entry.Dependency;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Data
public class CompileInfo {

    private String activator;
    private CompileType type;
    private String name;
    private String symbolicName;
    private String packageName;
    @SerializedName("manifest")
    private Map<String,Object> manifestTemplate = Collections.emptyMap();
    private String output;
    private String manifestDirectory = "META-INF";
    private List<String> dependencyExclude = Collections.emptyList();
    private List<String> importPackageExclude = Arrays.asList(
            "^javax.security.*$",
            "^org.osgi.util.*$",
            "org.apache.felix",
            "^net.sf.cglib.*$",
            "^org.apache.tools.*$"
    );

    public void setImportPackageExclude(List<String> exclude){
        exclude.addAll(this.importPackageExclude);
        this.importPackageExclude = exclude.stream().distinct().collect(Collectors.toList());
    }

    public boolean matchDependencyExclude(Dependency dependency){
        String name = dependency.getName();
        return this.dependencyExclude.stream()
                .noneMatch(exclude ->  Pattern.matches(exclude,name));
    }
    public boolean matchImportPackageExclude(String packageName){
        return this.importPackageExclude.stream()
                .noneMatch(exclude ->  Pattern.matches(exclude,packageName));
    }

    public void check() throws CompileInfoCheckException {
        if (this.type == null){
            throw new CompileInfoCheckException("type");
        }
        if (StringUtils.isEmpty(this.packageName)){
            throw new CompileInfoCheckException("packageName");
        }
        if (StringUtils.isEmpty(this.name)){
            throw new CompileInfoCheckException("name");
        }
    }

}
