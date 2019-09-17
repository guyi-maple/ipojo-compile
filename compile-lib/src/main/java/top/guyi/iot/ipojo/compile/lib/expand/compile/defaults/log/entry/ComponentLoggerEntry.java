package top.guyi.iot.ipojo.compile.lib.expand.compile.defaults.log.entry;

import lombok.AllArgsConstructor;
import lombok.Data;
import top.guyi.iot.ipojo.compile.lib.compile.entry.CompileClass;
import top.guyi.iot.ipojo.compile.lib.expand.compile.defaults.log.entry.LoggerEntry;

import java.util.List;

@Data
@AllArgsConstructor
public class ComponentLoggerEntry {

    private CompileClass component;
    private List<LoggerEntry> loggerEntry;

}
