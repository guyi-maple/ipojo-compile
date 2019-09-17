package top.guyi.iot.ipojo.compile.lib.expand.compile.defaults.configuration.entry;

import javassist.CtClass;
import javassist.CtField;
import lombok.AllArgsConstructor;
import lombok.Data;
import top.guyi.iot.ipojo.application.osgi.configuration.annotation.ConfigurationKey;

@Data
@AllArgsConstructor
public class ConfigurationField {

    private CtClass classes;
    private CtField field;
    private ConfigurationKey key;

}
