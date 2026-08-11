# xfg-ddd-gateway

## 启动前准备

启动前必须通过环境变量注入注册中心和 RBAC 管理员凭据，配置没有任何外部服务或密码默认值：

```bash
export DUBBO_REGISTRY_ADDRESS='nacos://127.0.0.1:8848'
export DUBBO_REGISTRY_USERNAME='nacos-user'
export DUBBO_REGISTRY_PASSWORD='从密钥管理系统读取'
export DUBBO_CONFIG_CENTER_ADDRESS='nacos://127.0.0.1:8848'
export RBAC_ADMIN_USERNAME='rbac-admin'
export RBAC_ADMIN_PASSWORD='从密钥管理系统读取'
```

同时启动 `xfg-ddd-archetype-std` 并发布 `cn.bugstack.api.IRbacService:1.0.0`。

## 编译

```bash
mvn -q -DskipTests -f /Users/ianqian/IdeaProjects/ddd/xfg-ddd-gateway/pom.xml package
```

## 启动

```bash
mvn -q -f /Users/ianqian/IdeaProjects/ddd/xfg-ddd-gateway/pom.xml spring-boot:run
```

## 调用示例

```bash
curl --user "$RBAC_ADMIN_USERNAME:$RBAC_ADMIN_PASSWORD" \
  "http://127.0.0.1:8092/api/rbac/users?pageNum=1&pageSize=20"
```

健康检查无需认证：

```bash
curl "http://127.0.0.1:8092/actuator/health"
```
