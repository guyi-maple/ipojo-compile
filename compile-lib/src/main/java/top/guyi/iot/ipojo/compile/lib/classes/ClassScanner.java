package top.guyi.iot.ipojo.compile.lib.classes;

import com.google.gson.Gson;
import javassist.CtClass;
import org.apache.commons.io.IOUtils;
import top.guyi.iot.ipojo.application.annotation.Component;
import javassist.ClassPool;
import javassist.NotFoundException;
import top.guyi.iot.ipojo.application.utils.StringUtils;
import top.guyi.iot.ipojo.compile.lib.compile.entry.CompileClass;
import top.guyi.iot.ipojo.compile.lib.compile.entry.ComponentEntry;
import top.guyi.iot.ipojo.compile.lib.compile.entry.ComponentInfo;
import top.guyi.iot.ipojo.compile.lib.configuration.Compile;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class ClassScanner {

    private Gson gson = new Gson();

    private Set<File> getClassFile(File root,Set<File> files){
        if (root.isDirectory()){
            Optional.ofNullable(root.listFiles())
                    .ifPresent(fs -> {
                        for (File file : fs) {
                            this.getClassFile(file,files);
                        }
                    });
        }else if (root.isFile() && root.getName().endsWith(".class")){
            files.add(root);
        }
        return files;
    }

    public Set<CompileClass> scan(ClassPool pool,Compile compile) throws IOException, NotFoundException {
        String path = compile.getProject().getWork();
        Set<File> files = this.getClassFile(new File(path),new HashSet<>());

        Set<CompileClass> components = new HashSet<>();

        for (File file : files) {
            String absolute = file.getAbsolutePath().replace("\\","/");
            components.add(new CompileClass(pool.get(
                    absolute
                            .replace(path,"")
                            .replace("/",".")
                            .replace(".class","")
            )));
        }

        Enumeration<URL> enumeration = pool.getClassLoader().getResources("component.info");
        while (enumeration.hasMoreElements()){
            URL url = enumeration.nextElement();
            String json = IOUtils.toString(url.openStream(), StandardCharsets.UTF_8);
            ComponentInfo componentInfo = this.gson.fromJson(json,ComponentInfo.class);
            if (!componentInfo.getName().equals(compile.getName())){
                if (componentInfo.getComponents() != null){
                    for (ComponentEntry component : componentInfo.getComponents()) {
                        CtClass classes = pool.get(component.getClasses());
                        components.add(new CompileClass(classes,false,true,component.isProxy()));
                    }
                }
                if (componentInfo.getUseComponents() != null){
                    for (ComponentEntry component : componentInfo.getUseComponents()) {
                        compile.addUseComponent(pool.get(component.getClasses()));
                    }
                }
            }
        }

        return components;
    }

    public Set<CompileClass> getComponent(ClassPool pool, Compile compile) throws IOException, NotFoundException {
        return this.scan(pool,compile)
                .stream()
                .map(classes -> {
                    try {
                        Component component = (Component) classes.getClasses().getAnnotation(Component.class);
                        if (component != null){
                            classes.setProxy(component.proxy());
                            classes.setOrder(component.order());
                            if (!StringUtils.isEmpty(component.name())){
                                classes.setName(component.name());
                            }
                            return classes;
                        }
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

}
