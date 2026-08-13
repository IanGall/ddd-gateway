# scaffold-gateway 网关骨架

本模块发布通用网关 Maven Archetype：

```text
cn.iantech:scaffold-gateway:1.0-SNAPSHOT
```

## 构建验证

```bash
mvn clean verify
```

该命令会生成测试网关工程，并执行生成工程的编译、上下文测试和 HTTP 安全链路测试。

## 安装与使用

```bash
mvn clean install

mvn archetype:generate \
  -DarchetypeCatalog=local \
  -DarchetypeGroupId=cn.iantech \
  -DarchetypeArtifactId=scaffold-gateway \
  -DarchetypeVersion=1.0-SNAPSHOT \
  -DgroupId=cn.example \
  -DartifactId=demo-gateway \
  -DrootArtifactId=demo-gateway \
  -DuAppName=DemoGateway \
  -Dversion=1.0.0-SNAPSHOT \
  -Dpackage=cn.example.gateway \
  -DinteractiveMode=false
```

生成工程继承 `gateway-parent`，默认包含 Spring MVC、Spring Security、参数校验、Actuator、Dubbo Triple、Nacos 和 `ddd-common`，不包含具体业务 API。
