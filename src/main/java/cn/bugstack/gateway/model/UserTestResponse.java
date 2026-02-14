package cn.bugstack.gateway.model;

public record UserTestResponse(
        String code,
        String message,
        String request,
        String rpcResult,
        long timestamp
) {
}
