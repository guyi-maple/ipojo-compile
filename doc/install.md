
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
            <groupId>top.guyi.iot.ipojo.compile</groupId>
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

### 编译及打包

pom.xml 配置完成后，正常运行 mvn compile , mvn package 即可。