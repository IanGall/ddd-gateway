# scaffold-gateway 模块协作说明

## 模块定位

- 本模块是通用网关 Maven Archetype，不承载具体业务实现。
- 模板必须能够独立生成、编译和测试，不依赖当前 RBAC API。

## 模板约束

- 统一继承 `ddd-base` 并导入 `ddd-base-bom`，不得重复维护依赖版本。
- RPC 协议统一使用 Dubbo Triple，业务契约由生成后的工程自行引入。
- 公共模型复用 `ddd-common`，禁止引入 Hutool 或创建重复公共模块。
- 所有环境变量占位符必须通过 Archetype 集成测试确认生成结果正确。
- 生成工程的租户配置使用 `GATEWAY_ADMIN_TENANT_ID`；Dubbo Triple 使用明文 RPC，注册中心凭据仍通过环境变量注入。

## 提交前检查

- 执行 `mvn clean verify`，确保生成工程完成编译和测试。
- 扫描生成结果，确认不存在模板变量、RBAC 业务依赖或真实凭据残留。
