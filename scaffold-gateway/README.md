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
标准工程 `IAuthService` 认证契约。认证和设备会话由 Auth 服务实现，RBAC 与 Customer 作为身份校验提供方；Gateway 分别通过
`/api/admin/auth/**` 和
`/api/app/auth/**` 转发认证请求，并按请求调用 Auth 校验 opaque Token；`/api/external/**` 固定使用渠道 HMAC，不复制具体 RBAC
管理接口。

生成工程只需配置 Auth 服务可访问的 Dubbo/Nacos 信息。主账号创建接口把 `X-Platform-Token` 转发给独立的
`IPlatformAccountService`，平台凭据由 Provider 最终校验；Gateway 不保存平台令牌。租户边界来自 Auth 校验后的主账号
ID，不再使用固定租户配置。Dubbo 消费端使用明文 Triple，注册中心通过
`DUBBO_REGISTRY_USERNAME` 和 `DUBBO_REGISTRY_PASSWORD` 认证。

认证 RPC 的异常由 `GatewayAuthClient` 沿 cause 链保留 `AppException`，未声明的 RPC 失败统一转换为
`AUTH_UNAVAILABLE`。过滤器将异常委托给唯一的 `GatewayExceptionHandler`，由语义码统一决定 HTTP 状态和 `data: null`
响应；业务代码不得手写 JSON、解析 Dubbo `GenericException` 或自行维护状态码映射。
