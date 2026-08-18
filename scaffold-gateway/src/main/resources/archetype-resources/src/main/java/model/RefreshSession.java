package ${package}.model;

import java.io.Serializable;
import java.time.Instant;

/** 一个设备上的可刷新登录会话。 */
public record RefreshSession(
        String sessionId,
        String familyId,
        Long accountId,
        Long userId,
        String username,
        String userType,
        String currentTokenHash,
        Instant createTime,
        Instant expireTime,
        Instant revokeTime,
        String clientType,
        String deviceId,
        String ipAddress,
        String userAgent) implements Serializable {

    public boolean isActive(Instant now) {
        return revokeTime == null && expireTime.isAfter(now);
    }

    public RefreshSession rotate(String tokenHash, ClientMetadata metadata) {
        return new RefreshSession(sessionId, familyId, accountId, userId, username, userType, tokenHash,
                createTime, expireTime, revokeTime, metadata.clientType(), metadata.deviceId(),
                metadata.ipAddress(), metadata.userAgent());
    }

    public RefreshSession revoke(Instant now) {
        return new RefreshSession(sessionId, familyId, accountId, userId, username, userType, currentTokenHash,
                createTime, expireTime, now, clientType, deviceId, ipAddress, userAgent);
    }

    public record ClientMetadata(
            String clientType,
            String deviceId,
            String ipAddress,
            String userAgent) implements Serializable {
    }
}
