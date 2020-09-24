
## Maven项目中使用

### pom.xml配置

#### 修改打包类型

``` xml
<packing>ipojo-bundle</packing>
```

#### 加入插件配置

``` xml
<build>
    <plugins>
        <plugin>
            <groupId>top.guyi.ipojo.compile</groupId>
            <artifactId>compile-maven-plugin</artifactId>
            <version>1.0.0.2</version>
            <extensions>true</extensions>
        </plugin>
    </plugins>
</build>
```

#### 私服配置

此项目暂时还未放入Maven中央仓库中，使用前需要添加Nexus私服

``` xml
<pluginRepositories>
    <pluginRepository>
        <id>iot</id>
        <url>http://nexus.guyi-maple.top/content/repositories/iot/</url>
    </pluginRepository>
</pluginRepositories>
```

### 编译配置文件 

使用ipojo-compile打包Bundle需要在项目中存在编译配置文件 <code>ipojo.compile</code> ， 文件格式为JSON

编译配置可以存放在项目根目录中，也可以存放在 <code>classpath</code> 路径下

#### 必须字段

* name - Bundle名称
* package - Bundle根包名
* type - 打包类型

以上字段为<code>ipojo.compile</code>文件的必要字段

#### 详细

更多关于编译配置文件的说明，请参见 [编译配置文件](configuration.md)

#### 示例

``` json
{
    "name": "test-bundle",
    "package": "tech.guyi.ipojo.test",
    "type": "bundle"
}
```

### 编译及打包

配置工作完成后，正常运行 mvn compile , mvn package 即可。