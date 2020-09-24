package tech.guyi.ipojo.compile.lib.expand.compile.defaults.log.entry;

import javassist.CtField;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoggerEntry {

    private CtField field;
    private String loggerName;

}
