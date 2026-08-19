# ${uAppName} 网关

这是一个基于 Spring Boot、Dubbo Triple 和 Nacos 的单模块网关骨架，认证契约对接标准工程 `IAuthService`。

## 启动

所有环境必须设置 `DUBBO_REGISTRY_ADDRESS`、`DUBBO_REGISTRY_USERNAME` 和 `DUBBO_REGISTRY_PASSWORD`。Access Token、 Refresh
Token 与 Session 均由 RBAC Auth 服务保存，Gateway 不连接 Redis。

```bash
mvn spring-boot:run
```

`GET /actuator/health` 为公开健康检查。`POST /auth/login` 返回 opaque Access Token 和 Refresh Token；业务请求只在
`Authorization: Bearer <accessToken>` 中携带 Access Token，Refresh Token 仅允许通过 `POST /auth/refresh` 的 JSON 请求体使用。

```bash
LOGIN_RESPONSE=$(curl -s -X POST http://127.0.0.1:8092/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"loginName":"用户名@主账号ID.com","password":"账号密码"}')
ACCESS_TOKEN=$(printf '%s' "$LOGIN_RESPONSE" | jq -r '.data.accessToken')
REFRESH_TOKEN=$(printf '%s' "$LOGIN_RESPONSE" | jq -r '.data.refreshToken')
curl -H "Authorization: Bearer $ACCESS_TOKEN" \
  http://127.0.0.1:8092/api/status

curl -s -X POST http://127.0.0.1:8092/auth/refresh \
  -H 'Content-Type: application/json' \
  -d "{\"refreshToken\":\"$REFRESH_TOKEN\"}"
```

每次刷新和业务请求都会调用 Auth 校验主账号、子账号状态和会话状态，并轮换 Refresh Token；旧 Refresh Token 重放会撤销
整个令牌族。`POST /auth/logout`、`POST /auth/logout-all`、`GET /auth/sessions` 和
`DELETE /auth/sessions/{sessionId}` 用于管理设备会话，均要求有效 Access Token。

生成网关只信任 Auth 返回的身份，并恢复主账号 ID、当前用户 ID 和本地用户名，不信任外部 `X-Tenant-Id`、`X-User-Id`。
`POST /platform/accounts` 用于创建主账号。Gateway 只把 `X-Platform-Token` 和开户字段转发给独立的
`IPlatformAccountService`，平台凭据由 Provider 最终校验，Gateway 不保存或比较凭据。Dubbo Triple 消费端使用明文 RPC， 注册中心通过
Nacos 用户名密码认证。

登录限流或临时锁定统一返回 `AUTH_RATE_LIMITED` 和 HTTP 429，不暴露具体触发条件。

认证 RPC 的异常由 `GatewayAuthClient` 沿 cause 链保留 `AppException`，未声明的 RPC 失败统一转换为
`AUTH_UNAVAILABLE`。过滤器将异常委托给唯一的 `GatewayExceptionHandler`，由 `Constants.ResponseCode` 统一决定 HTTP 状态和
`data: null` 响应；不要在过滤器或控制器中手写 JSON、解析 Dubbo `GenericException` 或重复维护状态码映射。
