# ${uAppName} 网关

这是一个基于 Spring Boot、Dubbo Triple 和 Nacos 的单模块网关骨架。

## 启动

开发环境可直接启动，生产环境必须设置 `DUBBO_REGISTRY_ADDRESS`、`DUBBO_REGISTRY_USERNAME`、`DUBBO_REGISTRY_PASSWORD` 和
`GATEWAY_ADMIN_TENANT_ID`。管理员账号和密码由标准工程数据库中的 `rbac_user` 提供。

```bash
mvn spring-boot:run
```

`GET /actuator/health` 为公开健康检查。先调用 `POST /auth/login` 获取 Sa-Token，再通过 `Authorization: Bearer <token>`
请求头访问 `GET /api/status`。业务网关接口应定义明确的 Dubbo API DTO，并在 Controller 中通过 `@DubboReference` 调用。

```bash
TOKEN=$(curl -s -X POST http://127.0.0.1:8092/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"数据库管理员账号","password":"数据库管理员密码"}' \
  | jq -r '.data.token')
curl -H "Authorization: Bearer $TOKEN" \
  http://127.0.0.1:8092/api/status
```

生成网关只信任认证后的管理员账号及配置租户，不信任外部 `X-Tenant-Id`、`X-User-Id`。Dubbo Triple 消费端使用明文 RPC，注册中心通过
Nacos 用户名密码认证。
