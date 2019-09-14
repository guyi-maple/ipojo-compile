package top.guyi.iot.ipojo.compile.expand.component;

import com.google.gson.Gson;
import javassist.ClassPool;
import javassist.CtClass;
import org.apache.commons.io.IOUtils;
import top.guyi.iot.ipojo.compile.lib.compile.entry.CompileClass;
import top.guyi.iot.ipojo.compile.lib.configuration.CompileInfo;
import top.guyi.iot.ipojo.compile.lib.compile.entry.ComponentEntry;
import top.guyi.iot.ipojo.compile.lib.compile.entry.ComponentInfo;
import top.guyi.iot.ipojo.compile.lib.expand.CompileExpand;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Set;

public class DependencyComponentExpand implements CompileExpand {

    private Gson gson = new Gson();

    @Override
    public Set<CompileClass> execute(ClassPool pool, String path, CompileInfo compileInfo, Set<CompileClass> components) throws Exception {
        Enumeration<URL> enumeration = pool.getClassLoader().getResources("component.info");
        while (enumeration.hasMoreElements()){
            String json = IOUtils.toString(enumeration.nextElement().openStream(), StandardCharsets.UTF_8);
            ComponentInfo componentInfo = this.gson.fromJson(json,ComponentInfo.class);
            for (ComponentEntry component : componentInfo.getComponents()) {
                CtClass classes = pool.get(component.getClasses());
                components.add(new CompileClass(classes,false));
            }
        }
        return components;
    }

}
