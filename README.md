
Maven编译插件

* 使用字节码修改方式实现[ipojo](https://github.com/guyi-maple/ipojo.git)注解API的依赖注入
* profile功能，可通过profile配置文件实现打包时注入依赖 （多渠道打包）
* 根据编译配置文件生成 MANIFEST.MF 文件，生成时会自动寻找依赖包中的可继承编译配置文件 （自动Import、Export）
* 编译时会自动将依赖包复制到 lib 目录下

详细准确的文档还在慢慢磨 😂