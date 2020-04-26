package top.guyi.iot.ipojo.compile.lib.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

/**
 * @author guyi
 * Attach文件工具类
 */
public class AttachUtils {

    /**
     * 获取Attach输入流
     * @param loader 类加载器
     * @param base 工作目录
     * @param pathOrName 路径或名称
     * @return 输入流
     */
    public static Optional<InputStream> getProfileInputStream(ClassLoader loader,String base,String pathOrName) throws IOException {
        try {
            return Optional.ofNullable(new URL(pathOrName).openStream());
        } catch (MalformedURLException e) {
            File file = new File(String.format("%s/%s.attach",base,pathOrName));
            if (file.exists()){
                return Optional.of(new FileInputStream(file));
            }
            return Optional.ofNullable(loader.getResourceAsStream(String.format("%s.attach",pathOrName)));
        }
    }

}
