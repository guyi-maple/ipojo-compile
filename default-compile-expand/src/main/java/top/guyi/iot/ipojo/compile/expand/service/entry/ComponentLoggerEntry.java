package top.guyi.iot.ipojo.compile.expand.service.entry;

import lombok.AllArgsConstructor;
import lombok.Data;
import top.guyi.iot.ipojo.compile.lib.compile.entry.CompileClass;

import java.util.List;

@Data
@AllArgsConstructor
public class ComponentLoggerEntry {

    private CompileClass component;
    private List<LoggerEntry> loggerEntry;

}
