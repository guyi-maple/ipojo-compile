package top.guyi.iot.ipojo.compile.lib.compile;

import top.guyi.iot.ipojo.compile.lib.compile.entry.CompileInfo;

@FunctionalInterface
public interface CompileInfoSetter {

    CompileInfo set(CompileInfo info);

}
