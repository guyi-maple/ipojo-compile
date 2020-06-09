
子项目 [compile-maven-plugin](https://github.com/guyi-maple/ipojo-compile/tree/master/compile-maven-plugin) 提供了以Felix环境启动Bundle的Mojo

执行此操作后会将项目<code>target</code>目录下的所有jar包以Bundle的形式在Felix中启动

如果需要在Felix环境中加入额外的Bundle，可以使用<code>configuration.felix</code>配置文件

``` json
{
    "bundles": [
        {
            "groupId": "...",
             "artifactId": "...",
             "version": "..."
        }
    ]
}
```

其他自定义配置可以使用<code>config</code>字段，此字段配置值将会映射到Felix的<code>config.properties</code>文件中

``` json
{
    "config": {
        "org.osgi.framework.storage.clean": "onFirstInit"
    }
}
```