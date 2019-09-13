package top.guyi.iot.ipojo.compile.lib.manifest;

import org.apache.commons.io.FileUtils;
import top.guyi.iot.ipojo.compile.lib.compile.entry.CompileInfo;
import top.guyi.iot.ipojo.compile.lib.compile.entry.ProjectInfo;
import top.guyi.iot.ipojo.compile.lib.expand.ManifestExpand;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class ManifestWriter {

    public void write(CompileInfo compileInfo, ProjectInfo projectInfo, List<ManifestExpand> expands){
        try {
            String directory = compileInfo.getOutput() + "/" + compileInfo.getManifestDirectory();
            if (new File(directory).mkdirs()){
                PrintWriter writer = new PrintWriter(directory + "/MANIFEST.MF");
                expands.stream()
                        .map(expand -> expand.execute(compileInfo,projectInfo))
                        .filter(Objects::nonNull)
                        .flatMap(Collection::stream)
                        .forEach(manifest -> writer.println(manifest.format()));
                writer.flush();
                writer.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

}
