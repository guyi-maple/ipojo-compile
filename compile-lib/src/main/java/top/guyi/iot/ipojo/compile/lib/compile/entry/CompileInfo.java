package top.guyi.iot.ipojo.compile.lib.compile.entry;

import top.guyi.iot.ipojo.application.utils.StringUtils;
import top.guyi.iot.ipojo.compile.lib.compile.exception.CompileInfoCheckException;
import lombok.Data;
import top.guyi.iot.ipojo.compile.lib.enums.CompileType;

import java.util.LinkedHashSet;
import java.util.Set;

@Data
public class CompileInfo {

    private boolean delete;

    private CompileType type;

    private String name;
    private String packageName;

    private String output;

    private String manifestDirectory = "META-INF";

    private Set<Dependency> dependencies;
    public CompileInfo addDependency(Dependency dependency){
        if (this.dependencies == null){
            this.dependencies = new LinkedHashSet<>();
        }
        this.dependencies.add(dependency);
        return this;
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
