package tech.guyi.ipojo.compile.maven.mojo;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Mojo(name = "package", defaultPhase = LifecyclePhase.PACKAGE)
public class PackageMojo extends AbstractMojo {

    @Parameter(property = "project")
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        List<File> files = Optional
                .ofNullable(new File(this.project.getBuild().getOutputDirectory()).listFiles())
                .map(Arrays::asList)
                .orElseGet(Collections::emptyList);
        ZipOutputStream zos = null;
        try {
            File jar = new File(String.format(
                    "%s/%s.jar",
                    this.project.getBuild().getOutputDirectory().replace("classes",""),
                    this.project.getBuild().getFinalName()
            ));
            zos = new ZipOutputStream(new FileOutputStream(jar));
            for (File file : files) {
                compress(file,zos,file.getName());
            }
            this.project.getArtifact().setFile(jar);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (Objects.nonNull(zos)){
                try {
                    zos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private void compress(File source, ZipOutputStream zos,String name) throws IOException {
        if (source.isFile()){
            zos.putNextEntry(new ZipEntry(name));
            zos.write(IOUtils.toByteArray(new FileInputStream(source)));
            zos.closeEntry();
        } else {
            List<File> children = Optional.ofNullable(source.listFiles())
                    .map(Arrays::asList)
                    .orElseGet(Collections::emptyList);
            if (children.isEmpty()){
                zos.putNextEntry(new ZipEntry(name + "/"));
                zos.closeEntry();
            } else {
                for (File file : children) {
                    compress(file,zos,name + "/" + file.getName());
                }
            }
        }
    }

}
