package ${package}.model;

import java.io.Serializable;
import java.time.Instant;

/** Refresh Token 哈希对应的轮换状态，Redis 中不保存令牌明文。 */
public record RefreshTokenRecord(
        String tokenHash,
        String sessionId,
        String familyId,
        Status status,
        Instant expireTime,
        Instant revokeTime,
        String replacedByHash) implements Serializable {

    public enum Status {
        ACTIVE,
        ROTATED,
        REVOKED
    }

    public RefreshTokenRecord rotate(String nextHash) {
        return new RefreshTokenRecord(tokenHash, sessionId, familyId, Status.ROTATED,
                expireTime, revokeTime, nextHash);
    }

    public RefreshTokenRecord revoke(Instant now) {
        return new RefreshTokenRecord(tokenHash, sessionId, familyId, Status.REVOKED,
                expireTime, now, replacedByHash);
    }
}
