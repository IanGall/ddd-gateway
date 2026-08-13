# ian-ddd-gateway

本仓库同时维护可运行的网关参考应用和通用 Maven Archetype：

```text
ian-ddd-gateway/
├── gateway-app       # 现有 RBAC 网关参考应用
└── scaffold-gateway  # 通用网关 Maven Archetype
```

## 参考应用

### 启动前准备

生产环境必须通过环境变量注入注册中心和 RBAC 管理员凭据：

```bash
export DUBBO_REGISTRY_ADDRESS='nacos://127.0.0.1:8848'
export DUBBO_REGISTRY_USERNAME='nacos-user'
export DUBBO_REGISTRY_PASSWORD='从密钥管理系统读取'
export RBAC_ADMIN_USERNAME='rbac-admin'
export RBAC_ADMIN_PASSWORD='从密钥管理系统读取'
```

同时启动 `ian-ddd-archetype-std` 并发布 `cn.iantech.api.IRbacService:1.0.0`。

### 编译与测试

```bash
mvn -q -f /Users/ianqian/IdeaProjects/ddd/ian-ddd-gateway/gateway-app/pom.xml test
```

### 启动

```bash
mvn -q -f /Users/ianqian/IdeaProjects/ddd/ian-ddd-gateway/gateway-app/pom.xml spring-boot:run
```

### 调用示例

```bash
curl --user "$RBAC_ADMIN_USERNAME:$RBAC_ADMIN_PASSWORD" \
  "http://127.0.0.1:8092/api/rbac/users?pageNum=1&pageSize=20"
```

健康检查无需认证：

```bash
curl "http://127.0.0.1:8092/actuator/health"
```

## 通用网关骨架

骨架只包含 Web 接入、安全认证、参数校验、统一异常、Actuator、Dubbo Triple 消费端和 Nacos 配置，不绑定 RBAC 或其他业务 API。

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
