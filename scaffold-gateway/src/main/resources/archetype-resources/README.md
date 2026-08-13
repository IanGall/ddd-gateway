# ${uAppName} 网关

这是一个基于 Spring Boot、Dubbo Triple 和 Nacos 的单模块网关骨架。

## 启动

开发环境可直接启动，生产环境必须设置 `DUBBO_REGISTRY_ADDRESS`、`DUBBO_REGISTRY_USERNAME`、`DUBBO_REGISTRY_PASSWORD`、`GATEWAY_ADMIN_USERNAME` 和 `GATEWAY_ADMIN_PASSWORD`。

```bash
mvn spring-boot:run
```

`GET /actuator/health` 为公开健康检查，`GET /api/status` 需要使用管理员 Basic Auth。业务网关接口应定义明确的 Dubbo API DTO，并在 Controller 中通过 `@DubboReference` 调用。
