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

    public Set<CompileClass> scan(ClassPool pool, String path) throws IOException, NotFoundException {
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
            String json = IOUtils.toString(enumeration.nextElement().openStream(), StandardCharsets.UTF_8);
            ComponentInfo componentInfo = this.gson.fromJson(json,ComponentInfo.class);
            for (ComponentEntry component : componentInfo.getComponents()) {
                CtClass classes = pool.get(component.getClasses());
                components.add(new CompileClass(classes,false,true,component.isProxy()));
            }
        }

        return components;
    }

    public Set<CompileClass> getComponent(ClassPool pool,String path) throws IOException, NotFoundException {
        return this.scan(pool,path)
                .stream()
                .map(compile -> {
                    try {
                        Component component = (Component) compile.getClasses().getAnnotation(Component.class);
                        if (component != null){
                            compile.setProxy(component.proxy());
                            compile.setOrder(component.order());
                            if (!StringUtils.isEmpty(component.name())){
                                compile.setName(component.name());
                            }
                            return compile;
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
