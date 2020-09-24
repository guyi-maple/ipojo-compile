package top.guyi.ipojo.compile.lib.compile.exception;

public class CompileInfoFileNotFoundException extends Exception {

    public CompileInfoFileNotFoundException(){
        super("找不到编译配置文件 [ipojo.compile]");
    }

}
