package top.guyi.iot.ipojo.compile.lib.configuration;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import javassist.CtClass;
import javassist.CtField;
import javassist.NotFoundException;
import lombok.Data;
import org.apache.commons.io.IOUtils;
import top.guyi.iot.ipojo.application.annotation.DynamicInject;
import top.guyi.iot.ipojo.compile.lib.compile.entry.CompileClass;
import top.guyi.iot.ipojo.compile.lib.configuration.entry.CompileExclude;
import top.guyi.iot.ipojo.compile.lib.configuration.entry.Dependency;
import top.guyi.iot.ipojo.compile.lib.enums.CompileType;
import top.guyi.iot.ipojo.compile.lib.configuration.entry.Project;
import top.guyi.iot.ipojo.compile.lib.enums.JdkVersion;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

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

    private Set<CtClass> useComponents = new HashSet<>();

    public void addUseComponent(CtClass classes){
        this.getUseComponents().add(classes);
    }

    public void addUseComponent(CtField field){
        try {
            this.addUseComponent(field.getType());
        } catch (NotFoundException e) {
            e.printStackTrace();
        }
    }

    public String getSymbolicName(){
        return Optional.ofNullable(this.symbolicName).orElse(this.name);
    }

    /**
     * 设置JDK版本
     * @param url Class文件URL
     * @throws IOException
     */
    public void formatJavaVersion(URL url) throws IOException {
        byte[] arr = IOUtils.toByteArray(url.openStream());
        arr[7] = (byte) this.jdk.getTarget();
        OutputStream out = new FileOutputStream(url.getFile());
        out.write(arr);
        out.flush();
        out.close();
    }

    public Set<CompileClass> filterUseComponents(Set<CompileClass> components){
        return components
                .stream()
                .filter(component -> {
                    if (component.getClasses().hasAnnotation(DynamicInject.class)){
                        try {
                            DynamicInject inject = (DynamicInject) component.getClasses().getAnnotation(DynamicInject.class);
                            if (inject.superEquals()){
                                for (CtClass useComponent : this.getUseComponents()) {
                                    if (component.getClasses().subtypeOf(useComponent)){
                                        return true;
                                    }
                                }
                                return false;
                            }else{
                                return this.getUseComponents().contains(component.getClasses());
                            }
                        } catch (ClassNotFoundException | NotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                    return true;
                })
                .collect(Collectors.toSet());
    }

}
