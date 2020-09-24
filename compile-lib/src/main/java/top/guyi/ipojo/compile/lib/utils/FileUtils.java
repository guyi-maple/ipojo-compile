package top.guyi.ipojo.compile.lib.utils;

import org.apache.commons.io.IOUtils;
import top.guyi.ipojo.compile.lib.configuration.entry.Project;

import java.io.*;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

/**
 * @author guyi
 * Attach文件工具类
 */
public class FileUtils {

    public static final String ATTACH_SUFFIX = ".attach";
    public static final String COMPILE_FILE_NAME = "ipojo.compile";

    public static Stream<String> getFileContents(String name,Project project){
        List<String> list = new LinkedList<>();
        File file = new File(String.format("%s/%s",project.getBaseDir(), name));
        if (file.exists()){
            try {
                list.add(IOUtils.toString(new FileInputStream(file), StandardCharsets.UTF_8));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        file = new File(String.format("%s/%s",project.getWork(), name));
        if (file.exists()){
            try {
                list.add(IOUtils.toString(new FileInputStream(file), StandardCharsets.UTF_8));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        project.getDependencies().stream()
                .map(d -> MavenUtils.get(project,d).orElse(null))
                .filter(Objects::nonNull)
                .map(path -> {
                    try {
                        JarFile jar = ((JarURLConnection) new URL(String.format("jar:file:%s!/",path)).openConnection()).getJarFile();
                        ZipEntry entry = jar.getEntry(name);
                        String data = null;
                        if (entry != null){
                            data = IOUtils.toString(jar.getInputStream(entry),StandardCharsets.UTF_8);
                        }
                        jar.close();
                        return data;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .forEach(list::add);

        return list.stream();
    }

    /**
     * 获取所有ipojo.compile文件内容
     * @param project 项目
     * @return 内容
     */
    public static List<String> getCompileFileContents(Project project){
        return getFileContents(COMPILE_FILE_NAME,project).collect(Collectors.toList());
    }

    /**
     * 获取Attach数据
     * @param pathOrName 路径或名称
     * @param project 项目
     * @return Attach数据
     */
    public static Optional<String> getAttachContent(String pathOrName, Project project) throws IOException {
        try {
            return Optional.ofNullable(new URL(pathOrName).openStream()).map(in -> {
                try {
                    return IOUtils.toString(in,StandardCharsets.UTF_8);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            });
        } catch (MalformedURLException e) {
            File file = new File(String.format("%s/%s%s",project.getBaseDir(),pathOrName,ATTACH_SUFFIX));
            if (file.exists()){
                return Optional.of(org.apache.commons.io.FileUtils.readFileToString(file, StandardCharsets.UTF_8));
            }

            file = new File(String.format("%s/%s%s",project.getWork(),pathOrName,ATTACH_SUFFIX));
            if (file.exists()){
                return Optional.of(org.apache.commons.io.FileUtils.readFileToString(file, StandardCharsets.UTF_8));
            }

            return getAttachContent(project,pathOrName);
        }
    }

    /**
     * 获取Attach内容
     * @param project 项目
     * @param pathOrName 名称或路径
     * @return Attach内容
     */
    public static Optional<String> getAttachContent(Project project, String pathOrName){
        return getFileContents(pathOrName + ATTACH_SUFFIX,project).findFirst();
    }

}
