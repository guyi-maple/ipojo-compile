package top.guyi.iot.ipojo.compile.expand.helper;

import top.guyi.iot.ipojo.compile.lib.project.entry.Dependency;

import java.io.*;
import java.util.*;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ImportHelper {

    private static Pattern pattern = Pattern.compile("((?!;)([a-zA-Z]+[.]?)+(?!=))");

    private static Set<String> match(String content){
        Set<String> result = new HashSet<>();
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()){
            result.add(matcher.group());
        }
        return result.stream()
                .filter(line -> line.contains("."))
                .collect(Collectors.toSet());
    }

    public static Set<String> getDependencyImportPackages(Set<Dependency> dependencies){
        return dependencies.stream()
                .map(dependency -> {
                    try {
                        ZipFile zf = new ZipFile(dependency.getPath());
                        return new Manifest(zf.getInputStream(new ZipEntry("META-INF/MANIFEST.MF")));
                    } catch (IOException e) {
                        e.printStackTrace();
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .map(manifest ->
                        Optional.ofNullable(manifest.getMainAttributes().getValue("Import-Package"))
                                .map(ImportHelper::match)
                                .orElseGet(HashSet::new))
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

}
