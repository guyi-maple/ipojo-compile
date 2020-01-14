package top.guyi.iot.ipojo.compile.lib.configuration;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import top.guyi.iot.ipojo.compile.lib.configuration.entry.CompileExclude;
import top.guyi.iot.ipojo.compile.lib.configuration.entry.Dependency;
import top.guyi.iot.ipojo.compile.lib.enums.CompileType;
import top.guyi.iot.ipojo.compile.lib.configuration.entry.Project;
import top.guyi.iot.ipojo.compile.lib.enums.JdkVersion;

import java.util.*;

@Data
public class Compile {

    @Expose
    @SerializedName("extends")
    private Set<String> extendsName;

    @Expose(serialize = false)
    private String activator;

    @Expose
    private JdkVersion jdk = JdkVersion.JAVA8;

    @Expose
    private boolean formatJdkVersion;

    @Expose
    private String name;

    @Expose
    private String symbolicName;

    @Expose
    private CompileType type;

    @Expose
    @SerializedName("package")
    private String packageName;

    @Expose
    private Set<String> modules = new HashSet<>();

    @Expose
    private Set<Dependency> dependencies = Collections.emptySet();

    @Expose
    @SerializedName("manifest")
    private Map<String,Object> manifestTemplate = Collections.emptyMap();

    @Expose(serialize = false)
    private Project project = new Project();

    @Expose
    private CompileExclude exclude = new CompileExclude();

    @Expose
    private Map<String,String> configuration = Collections.emptyMap();

    @Expose
    private Map<String,String> env = Collections.emptyMap();

    public String getSymbolicName(){
        return Optional.ofNullable(this.symbolicName).orElse(this.name);
    }

}
