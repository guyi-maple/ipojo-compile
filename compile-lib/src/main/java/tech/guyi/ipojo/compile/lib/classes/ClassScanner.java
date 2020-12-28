package tech.guyi.ipojo.compile.lib.classes;

import com.google.gson.Gson;
import javassist.CtClass;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.BooleanMemberValue;
import javassist.bytecode.annotation.IntegerMemberValue;
import javassist.bytecode.annotation.StringMemberValue;
import org.apache.commons.io.IOUtils;
import javassist.ClassPool;
import javassist.NotFoundException;
import tech.guyi.ipojo.compile.lib.compile.entry.CompileClass;
import tech.guyi.ipojo.compile.lib.compile.entry.ComponentEntry;
import tech.guyi.ipojo.compile.lib.compile.entry.ComponentInfo;
import tech.guyi.ipojo.compile.lib.configuration.Compile;
import tech.guyi.ipojo.compile.lib.configuration.entry.Dependency;
import tech.guyi.ipojo.compile.lib.cons.AnnotationNames;
import tech.guyi.ipojo.compile.lib.enums.CompileType;
import tech.guyi.ipojo.compile.lib.utils.AnnotationUtils;
import tech.guyi.ipojo.compile.lib.utils.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.*;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

public class ClassScanner {

    private final Gson gson = new Gson();

    private JarFile getJarFile(String path) throws IOException {
        return ((JarURLConnection)new URL(String.format("jar:file:%s!/",path)).openConnection()).getJarFile();
    }

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

    public Set<CompileClass> scan(ClassPool pool, Compile compile) throws IOException, NotFoundException {
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

        for (Dependency dependency : compile.getProject().getDependencies()) {
            Optional<String> dependencyPath = dependency.get(compile.getProject());
            if (dependencyPath.isPresent()){
                JarFile jar = this.getJarFile(dependencyPath.get());
                ZipEntry entry = jar.getEntry("component.info");
                if (entry != null){
                    ComponentInfo componentInfo = this.gson.fromJson(IOUtils.toString(jar.getInputStream(entry)),ComponentInfo.class);
                    if (!componentInfo.getName().equals(compile.getName())){
                        compile.getModules().add(componentInfo.getName());
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
            }
        }

        return components;
    }

    public Set<CompileClass> getComponent(ClassPool pool, Compile compile) throws IOException, NotFoundException {
        return this.scan(pool,compile)
                .stream()
                .map(component -> AnnotationUtils.getAnnotation(component.getClasses(), AnnotationNames.Component)
                        .map(annotation -> {
                            AnnotationUtils.getAnnotationValue(annotation,"proxy")
                                    .map(value -> (BooleanMemberValue) value)
                                    .map(BooleanMemberValue::getValue)
                                    .ifPresent(component::setProxy);
                            AnnotationUtils.getAnnotationValue(annotation,"order")
                                    .map(value -> (IntegerMemberValue) value)
                                    .map(IntegerMemberValue::getValue)
                                    .ifPresent(component::setOrder);
                            AnnotationUtils.getAnnotationValue(annotation,"name")
                                    .map(value -> (StringMemberValue) value)
                                    .map(StringMemberValue::getValue)
                                    .filter(name -> !StringUtils.isEmpty(name))
                                    .ifPresent(component::setName);
                            return component;
                        })
                        .orElse(null))
                .filter(Objects::nonNull)
                .filter(component -> {
                    if (compile.getType() == CompileType.COMPONENT){
                        return true;
                    }

                    Optional<Annotation> annotation = AnnotationUtils.getAnnotation(component.getClasses(), AnnotationNames.ConditionOnEnv);
                    if (!annotation.isPresent()){
                        return true;
                    }

                    String name = AnnotationUtils.getAnnotationValue(annotation.get(),"name")
                            .map(value -> (StringMemberValue) value)
                            .map(StringMemberValue::getValue)
                            .orElse(null);

                    String val = AnnotationUtils.getAnnotationValue(annotation.get(),"value")
                            .map(value -> (StringMemberValue) value)
                            .map(StringMemberValue::getValue)
                            .orElse(null);

                    if (StringUtils.isEmpty(name) || StringUtils.isEmpty(val)){
                        return true;
                    }

                    return val.equals(compile.getEnv().get(name));
                })
                .collect(Collectors.toSet());
    }

}
