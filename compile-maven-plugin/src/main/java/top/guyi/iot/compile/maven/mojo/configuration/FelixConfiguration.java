package top.guyi.iot.compile.maven.mojo.configuration;

import lombok.Data;
import top.guyi.iot.ipojo.application.utils.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class FelixConfiguration {

    private String felixDownloadUrl = "https://mirrors.tuna.tsinghua.edu.cn/apache//felix/org.apache.felix.main.distribution-6.0.3.zip";

    private List<String> bundles = Collections.emptyList();
    private String clean = "onFirstInit";
    private String actions = "install,start";
    private String bundleDir = "bundle";

    public void addDefaultBundles(List<String> defaults){
        defaults.addAll(this.bundles);
        this.bundles = defaults;
    }

    public Map<String,String> toMap(){
        Map<String,String> map = new HashMap<>();

        if (bundles != null && bundles.size() > 0){
            StringBuffer sb = new StringBuffer();
            bundles.forEach(line -> sb.append(line).append(" "));
            map.put("felix.auto.start.1",sb.toString());
        }

        if (!StringUtils.isEmpty(this.clean)){
            map.put("org.osgi.framework.storage.clean",this.clean);
        }

        if (!StringUtils.isEmpty(this.actions)){
            map.put("felix.auto.deploy.action",this.actions);
        }

        if (!StringUtils.isEmpty(this.bundleDir)){
            map.put("felix.auto.deploy.dir",this.bundleDir);
        }

        return map;
    }

}
