package tech.guyi.ipojo.compile.lib.compile;

import tech.guyi.ipojo.compile.lib.compile.entry.CompileClass;
import tech.guyi.ipojo.compile.lib.configuration.Compile;
import javassist.ClassPool;

import java.util.Set;

/**
 * 编译处理器
 */
public interface CompileTypeHandler {

    default int order(){
        return 999;
    }

    default boolean check(Compile compile){
        return true;
    }

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
