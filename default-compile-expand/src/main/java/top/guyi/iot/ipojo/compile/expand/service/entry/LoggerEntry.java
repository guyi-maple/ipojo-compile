package top.guyi.iot.ipojo.compile.expand.service.entry;

import javassist.CtField;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoggerEntry {

    private CtField field;
    private String loggerName;

}
