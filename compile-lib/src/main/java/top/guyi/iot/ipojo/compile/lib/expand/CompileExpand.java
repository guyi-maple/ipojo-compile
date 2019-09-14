package top.guyi.iot.ipojo.compile.lib.expand;

import top.guyi.iot.ipojo.compile.lib.compile.entry.CompileClass;
import top.guyi.iot.ipojo.compile.lib.configuration.CompileInfo;
import javassist.ClassPool;

import java.util.Set;

public interface CompileExpand {

    default int order(){
        return 999;
    }

    /**
     * 处理
     * @param pool
     * @param path 项目Class文件目录
     * @param compileInfo 编译信息
     * @param components 项目组件
     * @return 更新后的项目组件
     * @throws Exception
     */
    Set<CompileClass> execute(ClassPool pool, String path, CompileInfo compileInfo, Set<CompileClass> components) throws Exception;

}
