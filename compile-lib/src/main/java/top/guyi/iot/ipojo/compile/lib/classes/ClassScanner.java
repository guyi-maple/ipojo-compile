package top.guyi.iot.ipojo.compile.lib.classes;

import top.guyi.iot.ipojo.application.annotation.Component;
import javassist.ClassPool;
import javassist.NotFoundException;
import top.guyi.iot.ipojo.application.utils.StringUtils;
import top.guyi.iot.ipojo.compile.lib.compile.entry.CompileClass;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class ClassScanner {

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

    public Set<CompileClass> scan(ClassPool pool, String path){
        Set<File> files = this.getClassFile(new File(path),new HashSet<>());

        return files.stream()
                .map(file -> {
                    try {
                        String absolute = file.getAbsolutePath().replace("\\","/");
                        return new CompileClass(pool.get(
                                absolute
                                        .replace(path,"")
                                        .replace("/",".")
                                        .replace(".class","")
                        ));
                    } catch (NotFoundException e) {
                        e.printStackTrace();
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public Set<CompileClass> getComponent(ClassPool pool,String path){
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
