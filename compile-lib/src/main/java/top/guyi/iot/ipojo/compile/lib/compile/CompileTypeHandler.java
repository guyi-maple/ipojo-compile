package top.guyi.iot.ipojo.compile.lib.compile;

import top.guyi.iot.ipojo.compile.lib.compile.entry.CompileClass;
import top.guyi.iot.ipojo.compile.lib.configuration.Compile;
import javassist.ClassPool;
import top.guyi.iot.ipojo.compile.lib.enums.CompileType;

import java.util.Set;

/**
 * 编译处理器
 */
public interface CompileTypeHandler {

    /**
     * 编译类型
     * @return
     */
    CompileType forType();

    /**
     * 处理
     * @param pool
     * @param compile 编译信息
     * @param components 项目组件
     * @return 更新后的项目组件
     * @throws Exception
     */
    Set<CompileClass> handle(ClassPool pool, Compile compile, Set<CompileClass> components) throws Exception;

}
