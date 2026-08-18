# ian-ddd-gateway

本仓库同时维护可运行的网关参考应用和通用 Maven Archetype：

```text
ian-ddd-gateway/
├── gateway-app       # 现有 RBAC 网关参考应用
└── scaffold-gateway  # 通用网关 Maven Archetype
```

## 参考应用

### 启动前准备

生产环境必须通过环境变量注入注册中心和平台级开户令牌：

```bash
export DUBBO_REGISTRY_ADDRESS='nacos://127.0.0.1:8848'
export DUBBO_REGISTRY_USERNAME='nacos-user'
export DUBBO_REGISTRY_PASSWORD='从密钥管理系统读取'
export PLATFORM_ADMIN_TOKEN='从密钥管理系统读取的高强度令牌'
export AUTH_REDIS_ADDRESS='redis://redis.example.internal:6379'
export AUTH_REDIS_PASSWORD='从密钥管理系统读取'
```

同时启动 `ian-ddd-archetype-std` 并发布 `cn.iantech.api.IRbacService:1.0.0`。

默认、开发和生产 profile 均启用 Redis 共享会话；必须保证 Redis 可连接并配置 `AUTH_REDIS_ADDRESS`。 仅测试场景通过测试属性显式关闭
Redis，生产运行路径不提供内存降级。

`POST /platform/accounts` 只接受 `X-Platform-Token`，生产环境未配置 `PLATFORM_ADMIN_TOKEN` 时应用拒绝启动。登录成功后，网关只从
Sa-Token Session 恢复主账号 ID、当前用户 ID 与本地用户名，不采信外部 `X-Tenant-Id` 或 `X-User-Id`。Dubbo Triple 使用明文
RPC 连接，注册中心仍使用 Nacos 用户名密码认证。

### 编译与测试

```bash
mvn -q -f /Users/ianqian/IdeaProjects/ddd/ian-ddd-gateway/gateway-app/pom.xml test
```

### 启动

```bash
mvn -q -f /Users/ianqian/IdeaProjects/ddd/ian-ddd-gateway/gateway-app/pom.xml spring-boot:run
```

### 开户与登录示例

```bash
curl -X POST http://127.0.0.1:8092/platform/accounts \
  -H 'Content-Type: application/json' \
  -H "X-Platform-Token: $PLATFORM_ADMIN_TOKEN" \
  -d '{"username":"root","password":"高强度主账号密码","displayName":"示例主账号"}'

LOGIN_RESPONSE=$(curl -s -X POST http://127.0.0.1:8092/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"loginName":"root@主账号ID.com","password":"高强度主账号密码"}' \
)
ACCESS_TOKEN=$(printf '%s' "$LOGIN_RESPONSE" | jq -r '.data.accessToken')
REFRESH_TOKEN=$(printf '%s' "$LOGIN_RESPONSE" | jq -r '.data.refreshToken')
curl -H "Authorization: Bearer $ACCESS_TOKEN" \
  "http://127.0.0.1:8092/api/rbac/users?pageNum=1&pageSize=20"

curl -s -X POST http://127.0.0.1:8092/auth/refresh \
  -H 'Content-Type: application/json' \
  -d "{\"refreshToken\":\"$REFRESH_TOKEN\"}"
```

业务请求需在 `Authorization` 请求头中携带 `Bearer <accessToken>`，Refresh Token 只提交给 `/auth/refresh`，不放入 URL
或业务请求头。
`/auth/logout`、`/auth/logout-all`、`/auth/sessions` 和 `/auth/sessions/{sessionId}` 用于退出和设备会话管理。

健康检查无需认证：

```bash
curl "http://127.0.0.1:8092/actuator/health"
```

## 通用网关骨架

骨架包含 Web 接入、安全认证、参数校验、统一异常、Actuator、Dubbo Triple 消费端和 Nacos 配置。认证契约明确绑定标准工程的
`IRbacService`，负责主账号/子账号登录、平台级开户和可信上下文恢复；具体 RBAC 管理接口仍由业务网关自行接入，不复制到骨架中。

### 构建与安装

```bash
mvn -f /Users/ianqian/IdeaProjects/ddd/ian-ddd-gateway/pom.xml clean verify
mvn -f /Users/ianqian/IdeaProjects/ddd/ian-ddd-gateway/scaffold-gateway/pom.xml clean install
```

### 生成网关工程

```bash
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

生成后执行：

```bash
cd demo-gateway
mvn clean package
```

接入业务 RPC 时，由网关工程依赖对应 DDD 服务的 `*-api` 制品，并在业务 Controller 中使用 `@DubboReference(protocol = "tri", retries = 0)` 调用。骨架不生成虚假的 RPC 接口或提供者。
