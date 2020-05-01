package top.guyi.iot.ipojo.compile.lib.utils;

import org.apache.commons.io.IOUtils;
import top.guyi.iot.ipojo.compile.lib.configuration.entry.Dependency;
import top.guyi.iot.ipojo.compile.lib.configuration.entry.Project;

import java.io.*;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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

    public static Stream<String> getFileContents(String name,String localRepository,Set<Dependency> dependencies){
        return dependencies.stream()
                .map(d -> MavenUtils.get(localRepository,d).orElse(null))
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
                .filter(Objects::nonNull);
    }

    /**
     * 获取所有ipojo.compile文件内容
     * @param localRepository 本地仓库路径
     * @param dependencies 依赖列表
     * @return 内容
     */
    public static List<String> getCompileFileContents(String localRepository,Set<Dependency> dependencies){
        return getFileContents(COMPILE_FILE_NAME,localRepository,dependencies).collect(Collectors.toList());
    }

    /**
     * 获取Attach数据
     * @param project 项目
     * @param pathOrName 路径或名称
     * @return Attach数据
     */
    public static Optional<String> getAttachContent(Project project, String pathOrName) throws IOException {
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

            return getAttachContent(project.getLocalRepository(),project.getDependencies(),pathOrName);
        }
    }

    /**
     * 获取Attach内容
     * @param localRepository 本地仓库路径
     * @param dependencies 依赖列表
     * @param pathOrName 名称或路径
     * @return Attach内容
     */
    public static Optional<String> getAttachContent(String localRepository, Set<Dependency> dependencies, String pathOrName){
        return getFileContents(pathOrName + ATTACH_SUFFIX,localRepository,dependencies).findFirst();
    }

}
