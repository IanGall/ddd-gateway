# xfg-ddd-gateway

## 启动前准备

1. 启动 Nacos，默认地址 `127.0.0.1:8848`
2. 启动 `xfg-ddd-archetype-std`，确保已发布 `cn.bugstack.api.IUserService:1.0.0`

## 编译

```bash
mvn -q -DskipTests -f /Users/ianqian/IdeaProjects/ddd/xfg-ddd-gateway/pom.xml package
```

## 启动

```bash
mvn -q -f /Users/ianqian/IdeaProjects/ddd/xfg-ddd-gateway/pom.xml spring-boot:run
```

## 测试接口

```bash
curl "http://127.0.0.1:8092/api/test/user?req=abc"
```
# ddd-gateway
