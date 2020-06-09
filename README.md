# ipojo-compile

使用字节码生成技术，实现 [ipojo](https://github.com/guyi-maple/ipojo) 及 [ipojo-module](https://github.com/guyi-maple/ipojo-module) 的API

### 功能实现

* Ipojo定义的接口及事件
* Ipojo-module定义的接口及事件
* 打包时将依赖添加到lib目录
* 自动及根据模板生成MANIFEST.INF文件
* 根据编译配置文件实现打包时修改依赖及配置，实现渠道包功能

### 子模块

* compile-lib 核心模块，主要功能实现
* compile-maven-plugin Maven插件，实际使用时使用此模块打包出来的Maven插件

### 使用

具体使用方式见 [文档](https://github.com/guyi-maple/ipojo-compile/blob/master/doc/index.md)