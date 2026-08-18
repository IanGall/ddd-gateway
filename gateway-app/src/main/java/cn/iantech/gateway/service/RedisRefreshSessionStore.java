package cn.iantech.gateway.service;

import cn.iantech.common.exception.AppException;
import org.redisson.api.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 基于 Redis 的刷新会话存储，使用 Redisson 分布式锁串行化令牌轮换。
 */
public class RedisRefreshSessionStore implements RefreshSessionStore {

    private static final String PREFIX = "gateway:auth:refresh:";
    private static final Duration MINIMUM_TTL = Duration.ofSeconds(1);

    private final RedissonClient redissonClient;

    public RedisRefreshSessionStore(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public void save(RefreshSession session) {
        Duration ttl = ttl(session.expiresAt());
        RMap<String, String> values = redissonClient.getMap(sessionKey(session.sessionId()));
        values.putAll(toMap(session));
        values.expire(ttl);

        RBucket<String> tokenIndex = redissonClient.getBucket(tokenKey(session.refreshTokenHash()));
        tokenIndex.set(session.sessionId(), ttl);

        RSet<String> familyIndex = redissonClient.getSet(familyKey(session.familyId()));
        familyIndex.add(session.sessionId());
        // 不使用 expireIfGreater：该 API 会生成带 GT 参数的 PEXPIREAT 脚本，旧版 Redis 不支持。
        familyIndex.expire(ttl(session.expiresAt()));

        RSet<String> userIndex = redissonClient.getSet(userKey(session.accountId(), session.userId(), session.userType()));
        userIndex.add(session.sessionId());
        userIndex.expire(ttl(session.expiresAt()));
    }

    @Override
    public Optional<RefreshSession> findByTokenHash(String tokenHash) {
        String sessionId = redissonClient.<String>getBucket(tokenKey(tokenHash)).get();
        return sessionId == null ? Optional.empty() : findBySessionId(sessionId);
    }

    @Override
    public Optional<RefreshSession> findBySessionId(String sessionId) {
        Map<String, String> values = redissonClient.<String, String>getMap(sessionKey(sessionId)).readAllMap();
        return values.isEmpty() ? Optional.empty() : Optional.of(fromMap(values));
    }

    @Override
    public List<RefreshSession> findByFamilyId(String familyId) {
        return redissonClient.<String>getSet(familyKey(familyId)).readAll().stream()
                .map(this::findBySessionId)
                .flatMap(Optional::stream)
                .toList();
    }

    @Override
    public List<RefreshSession> findActiveByUser(Long accountId, Long userId, String userType) {
        Instant now = Instant.now();
        return redissonClient.<String>getSet(userKey(accountId, userId, userType)).readAll().stream()
                .map(this::findBySessionId)
                .flatMap(Optional::stream)
                .filter(session -> session.isActive(now))
                .toList();
    }

    @Override
    public <T> T withFamilyLock(String familyId, Supplier<T> action) {
        RLock lock = redissonClient.getLock(lockKey(familyId));
        boolean acquired = false;
        try {
            acquired = lock.tryLock(5, TimeUnit.SECONDS);
            if (!acquired) {
                throw new AppException("AUTH_REFRESH_BUSY", "刷新请求处理中，请稍后重试");
            }
            return action.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AppException("AUTH_REFRESH_BUSY", "刷新请求处理中，请稍后重试");
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private Map<String, String> toMap(RefreshSession session) {
        return Map.ofEntries(
                Map.entry("sessionId", session.sessionId()),
                Map.entry("familyId", session.familyId()),
                Map.entry("accountId", session.accountId().toString()),
                Map.entry("userId", session.userId().toString()),
                Map.entry("userType", session.userType()),
                Map.entry("refreshTokenHash", session.refreshTokenHash()),
                Map.entry("accessToken", session.accessToken()),
                Map.entry("clientType", session.clientType()),
                Map.entry("deviceId", session.deviceId()),
                Map.entry("ipAddress", session.ipAddress()),
                Map.entry("userAgent", session.userAgent()),
                Map.entry("createdAt", session.createdAt().toString()),
                Map.entry("expiresAt", session.expiresAt().toString()),
                Map.entry("revokedAt", nullable(session.revokedAt())),
                Map.entry("replacedBy", nullable(session.replacedBy()))
        );
    }

    private RefreshSession fromMap(Map<String, String> values) {
        return new RefreshSession(
                values.get("sessionId"), values.get("familyId"), Long.valueOf(values.get("accountId")),
                Long.valueOf(values.get("userId")), values.get("userType"), values.get("refreshTokenHash"),
                values.get("accessToken"), values.get("clientType"), values.get("deviceId"),
                values.get("ipAddress"), values.get("userAgent"), Instant.parse(values.get("createdAt")),
                Instant.parse(values.get("expiresAt")), instant(values.get("revokedAt")),
                emptyToNull(values.get("replacedBy")));
    }

    private Duration ttl(Instant expiresAt) {
        Duration duration = Duration.between(Instant.now(), expiresAt);
        return duration.isNegative() || duration.isZero() ? MINIMUM_TTL : duration;
    }

    private String nullable(Object value) {
        return value == null ? "" : value.toString();
    }

    private Instant instant(String value) {
        String nonEmpty = emptyToNull(value);
        return nonEmpty == null ? null : Instant.parse(nonEmpty);
    }

    private String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    private String sessionKey(String sessionId) {
        return PREFIX + "session:" + sessionId;
    }

    private String tokenKey(String tokenHash) {
        return PREFIX + "token:" + tokenHash;
    }

    private String familyKey(String familyId) {
        return PREFIX + "family:" + familyId;
    }

    private String userKey(Long accountId, Long userId, String userType) {
        return PREFIX + "user:" + accountId + ":" + userType + ":" + userId;
    }

    private String lockKey(String familyId) {
        return PREFIX + "lock:" + familyId;
    }
}
