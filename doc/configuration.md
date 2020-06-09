# 说明

使用ipojo-compile编译Bundle项目时，要求项目中必须存在编译配置文件 <code>ipojo.compile</code>

# 路径

<code>ipojo.compile</code> 必须存放在以下路径中的一个

* <code>classpath</code>
* 项目根目录

# 配置项

| 名称 | 含义 | 必须 |
| ---- | ---- | ---- |
| name | Bundle名称，此配置会被应用到MANIFEST.INF文件中的 <code>Bundle-SymbolicName</code> 、 <code>Bundle-Name</code> 字段中 |√|
| package | 项目根包名 |√|
| type |打包类型。bundle - Bundle包；component - 支持库 |√|
| jdk |强制将Class文件格式为指定的JDK版本。none - 不格式化；7 - Java7； 8 - Java8|
| configuration | | 项目配置，与[@ConfigurationKey](https://github.com/guyi-maple/ipojo/blob/master/src/main/java/top/guyi/iot/ipojo/application/osgi/configuration/annotation/ConfigurationKey.java) 结合使用|
| env |环境变量配置，此配置项中的值可以通过 [ApplicationContext](https://github.com/guyi-maple/ipojo/blob/master/src/main/java/top/guyi/iot/ipojo/application/ApplicationContext.java) .getEvn(key) 获取|
