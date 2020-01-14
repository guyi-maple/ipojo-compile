package top.guyi.iot.ipojo.compile.lib.expand.compile.defaults.configuration.entry;

import javassist.CtField;
import lombok.AllArgsConstructor;
import lombok.Data;
import top.guyi.iot.ipojo.application.osgi.configuration.annotation.ConfigurationKey;
import top.guyi.iot.ipojo.compile.lib.compile.entry.CompileClass;

import java.util.Objects;

@Data
@AllArgsConstructor
public class ConfigurationField {

    private CompileClass classes;
    private CtField field;
    private ConfigurationKey key;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConfigurationField that = (ConfigurationField) o;
        return field.equals(that.field);
    }

    @Override
    public int hashCode() {
        return Objects.hash(field);
    }
}
