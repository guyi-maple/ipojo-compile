package top.guyi.iot.ipojo.compile.lib.configuration;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import javassist.CtClass;
import javassist.CtField;
import javassist.NotFoundException;
import javassist.bytecode.annotation.BooleanMemberValue;
import lombok.Data;
import org.apache.commons.io.IOUtils;
import top.guyi.iot.ipojo.compile.lib.compile.entry.CompileClass;
import top.guyi.iot.ipojo.compile.lib.configuration.entry.CompileExclude;
import top.guyi.iot.ipojo.compile.lib.cons.AnnotationNames;
import top.guyi.iot.ipojo.compile.lib.enums.CompileType;
import top.guyi.iot.ipojo.compile.lib.configuration.entry.Project;
import top.guyi.iot.ipojo.compile.lib.enums.JdkVersion;
import top.guyi.iot.ipojo.compile.lib.expand.compile.defaults.configuration.entry.ConfigurationKeyEntry;
import top.guyi.iot.ipojo.compile.lib.utils.AnnotationUtils;
import top.guyi.iot.ipojo.compile.lib.utils.JavassistUtils;

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
    private JdkVersion jdk;

    @Expose
    private String name;

    @Expose
    private String symbolicName;

    @Expose
    private CompileType type;

    @Expose
    @SerializedName("package")
    private String packageName;

    private Set<String> modules = new HashSet<>();

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
    private Set<ConfigurationKeyEntry> configurationKeys = new HashSet<>();

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
        if (this.jdk != null){
            byte[] arr = IOUtils.toByteArray(url.openStream());
            arr[7] = (byte) this.jdk.getTarget();
            OutputStream out = new FileOutputStream(url.getFile());
            out.write(arr);
            out.flush();
            out.close();
        }
    }

    public Set<CompileClass> filterUseComponents(Set<CompileClass> components){
        return components
                .stream()
                .filter(component -> AnnotationUtils.getAnnotation(component.getClasses(), AnnotationNames.DynamicInject)
                        .map(annotation -> AnnotationUtils.getAnnotationValue(annotation,"superEquals")
                                .map(value -> (BooleanMemberValue) value)
                                .map(BooleanMemberValue::getValue)
                                .filter(b -> b)
                                .map(superEquals -> this.getUseComponents()
                                        .stream()
                                        .anyMatch(use -> JavassistUtils.equalsType(component.getClasses(),use)))
                                .orElseGet(() -> this.getUseComponents().contains(component.getClasses())))
                        .orElse(true))
                .collect(Collectors.toSet());
    }

}
