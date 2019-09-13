package top.guyi.iot.ipojo.compile.lib.compile;

import top.guyi.iot.ipojo.compile.lib.compile.entry.CompileInfo;
import javassist.ClassPool;
import javassist.CtClass;
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
     * @param path 项目Class文件目录
     * @param compileInfo 编译信息
     * @param components 项目组件
     * @return 更新后的项目组件
     * @throws Exception
     */
    Set<CtClass> handle(ClassPool pool, String path, CompileInfo compileInfo, Set<CtClass> components) throws Exception;

}
