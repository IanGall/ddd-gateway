package cn.bugstack.gateway.model;

public record ErrorResponse(
        String code,
        String message,
        String detail,
        long timestamp
) {
}
