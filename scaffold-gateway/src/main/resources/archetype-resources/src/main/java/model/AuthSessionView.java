package ${package}.model;

import java.time.Instant;

/** 返回给客户端的设备会话摘要，不暴露任何令牌。 */
public record AuthSessionView(
        String sessionId,
        boolean current,
        String clientType,
        String deviceId,
        String ipAddress,
        String userAgent,
        Instant createTime,
        Instant expireTime) {
}
