package top.guyi.iot.ipojo.compile.lib.configuration;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import top.guyi.iot.ipojo.compile.lib.configuration.entry.CompileExclude;
import top.guyi.iot.ipojo.compile.lib.enums.CompileType;
import top.guyi.iot.ipojo.compile.lib.configuration.entry.Project;

import java.util.*;

@Data
public class Compile {

    private String activator;
    private CompileType type;

    @SerializedName("package")
    private String packageName;

    @SerializedName("manifest")
    private Map<String,Object> manifestTemplate = Collections.emptyMap();

    private Project project = new Project();
    private CompileExclude exclude;

}
