# 网关骨架约定

- 仅在网关层处理 HTTP、认证、参数校验和统一异常响应。
- 下游调用使用 Dubbo Triple，消费端默认关闭启动检查。
- 传输对象必须定义明确的 Java 类型，不使用 Map。
- 生产环境凭据只能通过环境变量注入，禁止提交真实密钥。
- 主账号边界和当前用户身份必须由 Auth RPC 校验 opaque Token 后建立；RBAC 与 Customer 仅作为身份校验提供方，禁止信任外部身份
  Header。
- Gateway 不生成 Token、不保存 Session、不访问 Auth Redis；Admin/App 的 `/api/*/auth/**` 仅作为 Auth RPC 的 HTTP 门面。
- HTTP API 固定分为 `/api/admin/**`、`/api/app/**`、`/api/external/**`；App 用户不复用 Admin RBAC， Provider 必须按 customer
  binding 与资源归属完成最终授权。
- `/api/external/**` 只接受渠道 HMAC 与 `external:access` scope；`/api/admin/platform/accounts` 只转发
  `X-Platform-Token`，不得使用普通 Admin Bearer 替代平台凭据。
- RPC 异常必须由 `GatewayAuthClient` 转换为 `AppException`/`AUTH_UNAVAILABLE`，认证过滤器委托 `HandlerExceptionResolver`
  ，统一由 `GatewayExceptionHandler` 按 `Constants.ResponseCode` 输出状态码和 `data=null`；禁止手写 JSON、解析
  `GenericException` 或复制状态码映射。
- `X-Platform-Token` 仅转发给 `IPlatformAccountService`，Gateway 禁止保存或校验，且不得进入租户 RBAC 权限体系。
- Dubbo Triple 消费端使用明文 RPC，注册中心凭据通过环境变量注入。
- 新增代码和注释使用中文，优先复用 `ddd-common`。
