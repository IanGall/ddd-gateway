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

生成工程继承 `ddd-base` 并导入 `ddd-base-bom`，默认包含 Spring MVC、参数校验、Actuator、Dubbo Triple、Nacos、`ddd-common` 和
标准工程 `IAuthService` 认证契约。认证和设备会话由 RBAC Auth 服务实现，Gateway 只转发 `/auth/*` 并按请求调用 Auth 校验
opaque Token， 不复制具体 RBAC 管理接口。

生产环境只需配置 Auth 服务可访问的 Dubbo/Nacos 信息，并通过 `PLATFORM_ADMIN_TOKEN` 保护主账号创建接口。租户边界来自 Auth
校验后的 主账号 ID，不再使用固定租户配置。Dubbo 消费端使用明文 Triple，注册中心通过
`DUBBO_REGISTRY_USERNAME` 和 `DUBBO_REGISTRY_PASSWORD` 认证。
