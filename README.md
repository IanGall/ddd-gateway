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
```

同时启动 `ian-ddd-archetype-std` 并发布 `cn.iantech.api.IAuthService:1.0.0`。

认证会话由 RBAC Auth 服务统一保存和校验，Gateway 不再连接 Redis，也不保存本地 Session。

标准服务的 `IAuthService`、`IRbacService` 和 `IUserService` 全部显式声明 `throws AppException`。这样 Dubbo 会按声明式业务异常
原样传递语义码，Gateway 能区分令牌失效等业务失败与 Auth 服务不可用等基础设施故障。Gateway 的认证过滤器只负责把异常交给
Spring MVC 的统一异常解析器，不手写 JSON，也不分析 Dubbo 异常文本。

`POST /platform/accounts` 只接受 `X-Platform-Token`，生产环境未配置 `PLATFORM_ADMIN_TOKEN` 时应用拒绝启动。登录、刷新、注销和会话管理
均由 Gateway 转发给 RBAC Auth；业务请求携带的是由 Auth 签发的 opaque Bearer Token。Gateway 每个受保护请求调用 Auth 校验令牌后，
再恢复主账号和当前用户上下文，不采信外部 `X-Tenant-Id` 或 `X-User-Id`。Dubbo Triple 使用现有明文 RPC 连接，不启用 JWT、JWKS
或 mTLS。

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

HTTP 错误统一返回 `{"code","info","data"}`，并保留 `X-Request-Id`。公共语义码和状态码映射如下：

| 响应码                                | HTTP 状态码 |
|---------------------------------------|------------:|
| `INVALID_ARGUMENT`                    |         400 |
| `AUTH_REQUIRED`                       |         401 |
| `ACCESS_DENIED`                       |         403 |
| `AUTH_REFRESH_BUSY`                   |         409 |
| 其他明确业务异常                      |         422 |
| `RPC_ERROR`                           |         502 |
| `AUTH_UNAVAILABLE`、`RPC_NO_PROVIDER` |         503 |
| `RPC_TIMEOUT`                         |         504 |
| `INTERNAL_ERROR`                      |         500 |

客户端必须按 `SUCCESS` 等语义码判断结果，不再使用 `0000`～`0003` 数字码。认证失败只在 `AUTH_REQUIRED` 等业务码下返回；只有
无提供者、网络失败、超时或未识别的 Auth 运行时故障才返回服务不可用类错误。响应不会包含服务端堆栈。

健康检查无需认证：

```bash
curl "http://127.0.0.1:8092/actuator/health"
```

## 通用网关骨架

骨架包含 Web 接入、Auth RPC 认证、参数校验、统一异常、Actuator、Dubbo Triple 消费端和 Nacos 配置。认证契约明确绑定标准工程的
`IAuthService`，负责主账号/子账号登录、opaque Token 校验、会话管理和平台级开户；具体 RBAC 管理接口仍由业务网关自行接入，不复制到骨架中。

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
