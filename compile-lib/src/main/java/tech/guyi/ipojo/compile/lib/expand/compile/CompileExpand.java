package tech.guyi.ipojo.compile.lib.expand.compile;

import tech.guyi.ipojo.compile.lib.compile.entry.CompileClass;
import tech.guyi.ipojo.compile.lib.configuration.Compile;
import javassist.ClassPool;

import java.util.Set;

public interface CompileExpand {

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
    Set<CompileClass> execute(ClassPool pool, Compile compile, Set<CompileClass> components) throws Exception;

}
