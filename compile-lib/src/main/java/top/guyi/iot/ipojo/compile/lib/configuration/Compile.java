package top.guyi.iot.ipojo.compile.lib.configuration;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import top.guyi.iot.ipojo.compile.lib.configuration.entry.CompileExclude;
import top.guyi.iot.ipojo.compile.lib.enums.CompileType;
import top.guyi.iot.ipojo.compile.lib.configuration.entry.Project;

import java.util.*;

@Data
public class Compile {

    @Expose
    @SerializedName("extends")
    private Set<String> extendsName;

    @Expose(serialize = false)
    private String activator;

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
    @SerializedName("manifest")
    private Map<String,Object> manifestTemplate = Collections.emptyMap();

    @Expose(serialize = false)
    private Project project = new Project();

    @Expose
    private CompileExclude exclude = new CompileExclude();

    public String getSymbolicName(){
        return Optional.ofNullable(this.symbolicName).orElse(this.name);
    }

}
