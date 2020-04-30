package top.guyi.iot.ipojo.compile.lib.expand.compile.defaults.configuration.entry;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Objects;

@Data
@AllArgsConstructor
public class ConfigurationKeyEntry {

    private String key;
    private String remark;
    private boolean file;
    private String className;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConfigurationKeyEntry that = (ConfigurationKeyEntry) o;
        return key.equals(that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }
}
