# 说明

使用ipojo-compile编译Bundle项目时，要求项目中必须存在编译配置文件 <code>ipojo.compile</code>

# 路径

<code>ipojo.compile</code> 必须存放在以下路径中的一个

* <code>classpath</code>
* 项目根目录

# 配置项

| 配置项 | 含义 | 必须 |
| ---- | ---- | ---- |
| name | Bundle名称，此配置会被应用到MANIFEST.INF文件中的 <code>Bundle-SymbolicName</code> 、 <code>Bundle-Name</code> 字段中 |√|
| package | 项目根包名 |√|
| type |打包类型。bundle - Bundle包；component - 支持库 |√|
| jdk |强制将Class文件格式为指定的JDK版本。none - 不格式化；7 - Java7； 8 - Java8|
| configuration |项目配置，与[@ConfigurationKey](https://github.com/guyi-maple/ipojo/blob/master/src/main/java/top/guyi/iot/ipojo/application/osgi/configuration/annotation/ConfigurationKey.java) 结合使用|
| env |环境变量配置，此配置项中的值可以通过 [ApplicationContext](https://github.com/guyi-maple/ipojo/blob/master/src/main/java/top/guyi/iot/ipojo/application/ApplicationContext.java) .getEvn(key) 获取|
| manifest | MANIFEST.INF文件配置，此配置项中的字段将会应用到MANIFEST.INF文件中|
| exclude | 排除配置，参见[排除配置](## 排除配置)|
| project | 项目信息配置，参见[项目信息配置](#项目信息配置)|
| attach|附加编译配置文件名称(不包含文件后缀)，参见 [附加编译配置文件](## 附加编译配置文件)|

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


