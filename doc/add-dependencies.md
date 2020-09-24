
可以使用编译配置，在编译Bundle时加入依赖

``` json
{
  "project": {
    "dependencies": [
      {
        "groupId": "tech.guyi.ipojo.module",
        "artifactId": "coap-server",
        "version": "1.0.0.2-SNAPSHOT",
        "scope": "compile"
      }
    ]
  }
}
```

使用此配置，可以将<code>coap-server</code>的依赖加入到bundle中。

结合附加编译配置文件(attach)，可以实现打渠道包等功能。