package top.guyi.iot.ipojo.compile.lib.manifest;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class ManifestFile {

    public static void main(String[] args) throws IOException {
        Manifest manifest = new Manifest(new FileInputStream("D:\\Work\\Java\\ipojo-group\\ipojo-test\\MANIFEST.MF"));
        Attributes attributes = manifest.getMainAttributes();
        attributes.forEach((key, value) -> Arrays.stream(value.toString().split(",")).forEach(line -> System.out.println(line.trim())));
    }

}
