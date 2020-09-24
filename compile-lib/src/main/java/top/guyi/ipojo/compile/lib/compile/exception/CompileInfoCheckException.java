package top.guyi.ipojo.compile.lib.compile.exception;

public class CompileInfoCheckException extends Exception {

    public CompileInfoCheckException(String fieldName){
        super(String.format("配置项[%s]不能为空",fieldName));
    }

}
