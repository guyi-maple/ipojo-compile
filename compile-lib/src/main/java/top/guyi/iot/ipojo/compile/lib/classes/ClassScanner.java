package top.guyi.iot.ipojo.compile.lib.classes;

import top.guyi.iot.ipojo.application.annotation.Component;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

import java.io.File;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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

    public Set<CtClass> scan(ClassPool pool,String path){
        Set<File> files = this.getClassFile(new File(path),new HashSet<>());
        return files.stream()
                .map(file -> {
                    try {
                        return pool.get(
                                file.getAbsolutePath()
                                        .replace(path,"")
                                        .replace("/",".")
                                        .replace(".class","")
                        );
                    } catch (NotFoundException e) {
                        e.printStackTrace();
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public Set<CtClass> getComponent(ClassPool pool,String path){
        return this.scan(pool,path)
                .stream()
                .filter(classes -> classes.hasAnnotation(Component.class))
                .collect(Collectors.toSet());
    }

}
