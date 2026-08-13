package ${package}.model;

/** 网关基础状态，不依赖任何业务 API。 */
public record GatewayStatus(String application, String status) {
}
