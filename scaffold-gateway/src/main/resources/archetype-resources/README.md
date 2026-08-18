# ${uAppName} 网关

这是一个基于 Spring Boot、Dubbo Triple 和 Nacos 的单模块网关骨架，认证契约对接标准工程 `IRbacService`。

## 启动

开发环境可直接启动，生产环境必须设置 `DUBBO_REGISTRY_ADDRESS`、`DUBBO_REGISTRY_USERNAME`、`DUBBO_REGISTRY_PASSWORD` 和
`PLATFORM_ADMIN_TOKEN`。主账号保存在 `rbac_account`，子账号保存在 `rbac_user`。

```bash
mvn spring-boot:run
```

`GET /actuator/health` 为公开健康检查。先调用 `POST /auth/login` 获取 Sa-Token，再通过 `Authorization: Bearer <token>`
请求头访问 `GET /api/status`。业务网关接口应定义明确的 Dubbo API DTO，并在 Controller 中通过 `@DubboReference` 调用。

```bash
TOKEN=$(curl -s -X POST http://127.0.0.1:8092/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"loginName":"用户名@主账号ID.com","password":"账号密码"}' \
  | jq -r '.data.token')
curl -H "Authorization: Bearer $TOKEN" \
  http://127.0.0.1:8092/api/status
```

生成网关只信任认证后的 Token Session，并从中恢复主账号 ID、当前用户 ID 和本地用户名，不信任外部 `X-Tenant-Id`、`X-User-Id`。
`POST /platform/accounts` 由 `X-Platform-Token` 保护，用于创建主账号。Dubbo Triple 消费端使用明文 RPC，注册中心通过 Nacos
用户名密码认证。
