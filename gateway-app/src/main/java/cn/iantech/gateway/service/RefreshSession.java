package cn.iantech.gateway.service;

import java.io.Serializable;
import java.time.Instant;

/**
 * 刷新令牌对应的服务端可信会话。刷新令牌明文不会进入该对象。
 */
public record RefreshSession(
        String sessionId,
        String familyId,
        Long accountId,
        Long userId,
        String userType,
        String refreshTokenHash,
        String accessToken,
        String clientType,
        String deviceId,
        String ipAddress,
        String userAgent,
        Instant createdAt,
        Instant expiresAt,
        Instant revokedAt,
        String replacedBy
) implements Serializable {

    private static final long serialVersionUID = 1L;

    public boolean isActive(Instant now) {
        return revokedAt == null && replacedBy == null && expiresAt.isAfter(now);
    }

    public RefreshSession revoke(Instant now, String replacementSessionId) {
        return new RefreshSession(sessionId, familyId, accountId, userId, userType, refreshTokenHash,
                accessToken, clientType, deviceId, ipAddress, userAgent, createdAt, expiresAt,
                now, replacementSessionId);
    }
}
