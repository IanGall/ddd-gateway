# scaffold-gateway 网关骨架

本模块发布通用网关 Maven Archetype：

```text
cn.iantech:scaffold-gateway:1.0-SNAPSHOT
```

## 构建验证

```bash
mvn clean verify
```

该命令会生成测试网关工程，并执行生成工程的编译、上下文测试、HTTP 安全链路测试和 RPC 异常映射测试。

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

生成工程继承 `ddd-base` 并导入 `ddd-base-bom`，默认包含 Spring MVC、Sa-Token、参数校验、Actuator、Dubbo Triple、Nacos、
`ddd-common` 和标准工程 `IRbacService` 认证契约。骨架不复制具体 RBAC 管理接口。

生产环境通过 `PLATFORM_ADMIN_TOKEN` 保护主账号创建接口；租户边界来自认证后的主账号 ID，不再使用固定租户配置。Dubbo 消费端使用
明文 Triple，注册中心通过 `DUBBO_REGISTRY_USERNAME` 和 `DUBBO_REGISTRY_PASSWORD` 认证。
