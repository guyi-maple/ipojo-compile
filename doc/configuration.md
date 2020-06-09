# 说明

使用ipojo-compile编译Bundle项目时，要求项目中必须存在编译配置文件 <code>ipojo.compile</code>

# 路径

<code>ipojo.compile</code> 必须存放在以下路径中的一个

* <code>classpath</code>
* 项目根目录

# 配置项

| 配置项 | 含义 | 格式 |必须 |
| ---- | ---- | ---- | ----|
| name | Bundle名称，此配置会被应用到MANIFEST.INF文件中的 <code>Bundle-SymbolicName</code> 、 <code>Bundle-Name</code> 字段中|string |√|
| package | 项目根包名 |string|√|
| type |打包类型。bundle - Bundle包；component - 支持库 |string|√|
| jdk |强制将Class文件格式为指定的JDK版本。none - 不格式化；7 - Java7； 8 - Java8|string|
| configuration |项目配置，与[@ConfigurationKey](https://github.com/guyi-maple/ipojo/blob/master/src/main/java/top/guyi/iot/ipojo/application/osgi/configuration/annotation/ConfigurationKey.java) 结合使用|object|
| env |环境变量配置，此配置项中的值可以通过 [ApplicationContext](https://github.com/guyi-maple/ipojo/blob/master/src/main/java/top/guyi/iot/ipojo/application/ApplicationContext.java) .getEvn(key) 获取|object|
| manifest | MANIFEST.INF文件配置，此配置项中的字段将会应用到MANIFEST.INF文件中|object|
| exclude | 排除配置，参见[排除配置](#排除配置)|object|
| project | 项目信息配置，参见[项目信息配置](#项目信息配置)|object|
| attach|附加编译配置文件名称(不包含文件后缀)，参见 [附加编译配置文件](#附加编译配置文件)|array|

## 排除配置

| 配置项 | 含义|
|----|----|
|copy|复制依赖包到lib目录时排除的指定依赖，依赖名称格式为 <code>groupId:artifactId:version</code>， 支持正则表达式。|
|export|生成MANIFEST.INF文件中的Export-Package时排除指定包名，支持正则表达式。|
|import|生成MANIFEST.INF文件中的Import-Package时排除指定包名，支持正则表达式。|
|scope|复制依赖包到lib目录时排除生命周期为指定项的依赖。|

## 项目信息配置

打包Bundle时会自动注入项目信息，如果有特殊需求，可以通过编译配置文件进行覆盖。

| 配置项 | 含义|
|----|----|
| version | 版本号|
| finalName | 打包名称|
| baseDir | 项目路径 |
| sourceDir | 源码路径 |
| work | class文件路径|
| output | class保存路径|
|localRepository| 本地Maven仓库路径|
|servers|Maven服务器认证信息|

## 附加编译配置文件

除 <code>ipojo.compile</code> 文件外，还可以使用附加编译配置文件进行编译配置。

附件编译配置文件 <code>*.attach</code> ，格式、配置项与<code>ipojo.compile</code> 相同。

可在<code>ipojo.compile</code>中配置attach，选择需要使用的attach文件。

#### 存放路径

* <code>classpath</code>
* 项目根目录

# 配置继承

Bundle编译时会搜索所有的依赖，当依赖包中存在ipojo.compile文件时，会将其中的内容继承到当前项目的编译配置信息中。

当不想从依赖中继承编译配置时，可以使用<code>override</code>字段。

如项目本身的exclude配置为

``` json
{
    "exclude": {
        "scope": ["test","provide"]
    }
}
```

如果想保持此配置，不从依赖中继承，可写为

``` json
{
    "exclude": {
        "override": false,
        "value": {
            "scope": ["test","provide"]
        }
    }
}
```

# 配置值降级

当配置项类型为数组(array)时，如果只有一个值，支持直接写值，不需要使用数组

以下两种写法作用一致

``` json
{
    "manifest": {
        "Import-Package": ["top.guyi.test"]
    },
    "attach": "test"
}
```

``` json
{
    "manifest": {
        "Import-Package": "top.guyi.test"
    },
    "attach": ["test"]
}
```

# 范例

``` json
{
  "name": "iot-manager-plugin",
  "package": "com.robotaiot.iot.plugin.manager",
  "type": "bundle",
  "jdk": "7",
  "project": {
    "version": "1.3.0.0"
  },
  "configuration": {
    "vertx.server": "tcp://ihgu.cqccn.com:1883"
  },
  "attach": "igos",
  "manifest": {
    "Import-Package": [
      "javax.net",
      "javax.net.ssl",
      "javax.naming"
    ]
  },
  "env": {
    "thread.max.count": "3"
  }
}
```
