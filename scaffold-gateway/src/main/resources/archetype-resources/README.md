# ${uAppName} 网关

这是一个基于 Spring Boot、Dubbo Triple 和 Nacos 的单模块网关骨架，认证契约对接标准工程 `IAuthService`。

## 启动

所有环境必须设置 `DUBBO_REGISTRY_ADDRESS`、`DUBBO_REGISTRY_USERNAME` 和 `DUBBO_REGISTRY_PASSWORD`。Access Token、 Refresh
Token 与 Session 均由 Auth 服务保存，RBAC 与 Customer 作为身份校验提供方，Gateway 不连接 Redis。

```bash
mvn spring-boot:run
```

`GET /actuator/health` 为公开健康检查。管理端登录使用 `POST /api/admin/auth/login`，C 端注册/登录使用
`POST /api/app/auth/register`、`POST /api/app/auth/login`，均返回 opaque Access Token 和 Refresh Token；业务请求只在
`Authorization: Bearer <accessToken>` 中携带 Access Token，Refresh Token 仅允许通过对应端的 `/api/*/auth/refresh` JSON
请求体使用。

```bash
LOGIN_RESPONSE=$(curl -s -X POST http://127.0.0.1:8092/api/admin/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"loginName":"用户名@主账号ID.com","password":"账号密码"}')
ACCESS_TOKEN=$(printf '%s' "$LOGIN_RESPONSE" | jq -r '.data.accessToken')
REFRESH_TOKEN=$(printf '%s' "$LOGIN_RESPONSE" | jq -r '.data.refreshToken')
curl -H "Authorization: Bearer $ACCESS_TOKEN" \
  http://127.0.0.1:8092/api/admin/status

curl -s -X POST http://127.0.0.1:8092/api/admin/auth/refresh \
  -H 'Content-Type: application/json' \
  -d "{\"refreshToken\":\"$REFRESH_TOKEN\"}"
```

每次刷新和业务请求都会调用 Auth 校验主账号、子账号状态和会话状态，并轮换 Refresh Token；旧 Refresh Token 重放会撤销
整个令牌族。管理端与 C 端的注销、注销全部和设备会话接口分别位于 `/api/admin/auth/**`、`/api/app/auth/**`，均要求有效 Access
Token。

生成网关只信任 Auth 返回的身份，并恢复主账号 ID、当前用户 ID 和本地用户名，不信任外部 `X-Account-Id`、`X-User-Id`。
`POST /api/admin/platform/accounts` 用于创建主账号。Gateway 只把 `X-Platform-Token` 和开户字段转发给独立的
`IPlatformAccountService`，平台凭据由 Provider 最终校验，Gateway 不保存或比较凭据。Dubbo Triple 消费端使用明文 RPC， 注册中心通过
Nacos 用户名密码认证。

登录限流或临时锁定统一返回 `AUTH_RATE_LIMITED` 和 HTTP 429，不暴露具体触发条件。

`/api/external/**` 使用渠道 HMAC，不使用 Bearer Token。固定 scope 为 `external:access`。请求头固定为 `X-Channel-Code`、
`X-Channel-Secret-Version`、`X-Channel-Timestamp`、`X-Channel-Content-SHA256` 和 `X-Channel-Signature`。 Canonical Request
按 Method、Path、Query、Content-Type、渠道编码、密钥版本、时间戳和 Body SHA-256 八行组成， 签名算法固定为 HMAC-SHA256。请求体最大
1 MiB，时间窗为前后 300 秒；相同签名只能成功一次，重试必须更新时间戳并重新签名。

认证 RPC 的异常由 `GatewayAuthClient` 沿 cause 链保留 `AppException`，未声明的 RPC 失败统一转换为
`AUTH_UNAVAILABLE`。过滤器将异常委托给唯一的 `GatewayExceptionHandler`，由 `Constants.ResponseCode` 统一决定 HTTP 状态和
`data: null` 响应；不要在过滤器或控制器中手写 JSON、解析 Dubbo `GenericException` 或重复维护状态码映射。

## 业务流程测试

业务流程测试只允许放在网关层。工程通过 test scope 引入 `ddd-gateway-test-starter`，`GatewayBusinessWorkflowTest` 使用强类型
OpenFeign 接口和 DTO，在随机端口上验证登录、Token 传递及受保护接口，并通过 `@DetectNPlusOne` 约束重复 HTTP/RPC 调用。
新增流程应继续使用 `BusinessFlow` 和 `FlowKey<T>` 编排，不使用 `Map` 传输对象，也不增加自动重试。
