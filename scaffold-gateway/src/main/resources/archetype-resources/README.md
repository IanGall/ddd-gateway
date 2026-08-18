# ${uAppName} 网关

这是一个基于 Spring Boot、Dubbo Triple 和 Nacos 的单模块网关骨架，认证契约对接标准工程 `IRbacService`。

## 启动

开发环境可直接启动，生产环境必须设置 `DUBBO_REGISTRY_ADDRESS`、`DUBBO_REGISTRY_USERNAME`、`DUBBO_REGISTRY_PASSWORD`、
`REDIS_HOST` 和 `PLATFORM_ADMIN_TOKEN`。Access Token、Token Session 与 Refresh Session 都保存在 Redis，主账号保存在
`rbac_account`，子账号保存在 `rbac_user`。

```bash
mvn spring-boot:run
```

`GET /actuator/health` 为公开健康检查。`POST /auth/login` 返回 15 分钟 Access Token 和 30 天 Refresh Token；业务请求只在
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

每次刷新都会重新调用标准服务校验主账号、子账号状态和实时 RBAC 权限，并轮换 Refresh Token；旧 Refresh Token 重放会撤销
整个令牌族。`POST /auth/logout`、`POST /auth/logout-all`、`GET /auth/sessions` 和
`DELETE /auth/sessions/{sessionId}` 用于管理设备会话，均要求有效 Access Token。

生成网关只信任认证后的 Token Session，并从中恢复主账号 ID、当前用户 ID和本地用户名，不信任外部 `X-Tenant-Id`、
`X-User-Id`。`POST /platform/accounts` 由 `X-Platform-Token` 保护，用于创建主账号。Dubbo Triple 消费端使用明文 RPC， 注册中心通过
Nacos 用户名密码认证。
