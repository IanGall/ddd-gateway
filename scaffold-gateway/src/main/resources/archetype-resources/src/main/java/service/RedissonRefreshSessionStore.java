package ${package}.service;

import cn.iantech.common.exception.AppException;
import ${package}.config.GatewayTokenProperties;
import ${package}.model.RefreshSession;
import ${package}.model.RefreshTokenRecord;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RSetCache;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/** 基于 Redisson 的 Refresh Session、令牌族和用户索引存储。 */
public class RedissonRefreshSessionStore implements RefreshSessionStore {

    private static final String PREFIX = "gateway:refresh:";

    private final RedissonClient redissonClient;
    private final GatewayTokenProperties properties;

    public RedissonRefreshSessionStore(RedissonClient redissonClient, GatewayTokenProperties properties) {
        this.redissonClient = redissonClient;
        this.properties = properties;
    }

    @Override
    public RefreshTokenRecord findToken(String tokenHash) {
        return tokenBucket(tokenHash).get();
    }

    @Override
    public RefreshSession findSession(String sessionId) {
        return sessionBucket(sessionId).get();
    }

    @Override
    public void saveToken(RefreshTokenRecord token) {
        tokenBucket(token.tokenHash()).set(token, remaining(token.expireTime()));
        familyIndex(token.familyId()).add(token.tokenHash(), remainingSeconds(token.expireTime()), TimeUnit.SECONDS);
    }

    @Override
    public void saveSession(RefreshSession session) {
        sessionBucket(session.sessionId()).set(session, remaining(session.expireTime()));
    }

    @Override
    public void indexSession(RefreshSession session) {
        long ttl = remainingSeconds(session.expireTime());
        userIndex(session.accountId(), session.userType(), session.userId())
                .add(session.sessionId(), ttl, TimeUnit.SECONDS);
        accountIndex(session.accountId()).add(session.sessionId(), ttl, TimeUnit.SECONDS);
    }

    @Override
    public List<RefreshSession> findUserSessions(Long accountId, String userType, Long userId) {
        Instant now = Instant.now();
        return userIndex(accountId, userType, userId).stream()
                .map(this::findSession)
                .filter(Objects::nonNull)
                .filter(session -> session.isActive(now))
                .toList();
    }

    @Override
    public void revokeSession(String sessionId) {
        RefreshSession session = findSession(sessionId);
        if (session == null) {
            return;
        }
        withFamilyLock(session.familyId(), () -> {
            revokeFamilyWithoutLock(session.familyId());
            return null;
        });
    }

    @Override
    public void revokeFamily(String familyId) {
        withFamilyLock(familyId, () -> {
            revokeFamilyWithoutLock(familyId);
            return null;
        });
    }

    @Override
    public void revokeUser(Long accountId, String userType, Long userId) {
        userIndex(accountId, userType, userId).stream().toList().forEach(this::revokeSession);
    }

    @Override
    public void revokeAccount(Long accountId) {
        accountIndex(accountId).stream().toList().forEach(this::revokeSession);
    }

    @Override
    public <T> T withFamilyLock(String familyId, Supplier<T> operation) {
        RLock lock = redissonClient.getLock(PREFIX + "lock:family:" + familyId);
        boolean acquired = false;
        try {
            acquired = lock.tryLock(properties.getRefreshLockWait().toMillis(),
                    properties.getRefreshLockLease().toMillis(), TimeUnit.MILLISECONDS);
            if (!acquired) {
                throw new AppException("REFRESH_BUSY", "刷新请求处理中，请稍后重试");
            }
            return operation.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AppException("REFRESH_BUSY", "刷新请求处理中，请稍后重试");
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private void revokeFamilyWithoutLock(String familyId) {
        Instant now = Instant.now();
        List<RefreshTokenRecord> tokens = familyIndex(familyId).stream()
                .map(this::findToken)
                .filter(Objects::nonNull)
                .toList();
        tokens.stream().map(token -> token.revoke(now)).forEach(this::saveToken);
        tokens.stream().findFirst()
                .map(RefreshTokenRecord::sessionId)
                .map(this::findSession)
                .filter(Objects::nonNull)
                .map(session -> session.revoke(now))
                .ifPresent(this::saveSession);
    }

    private RBucket<RefreshTokenRecord> tokenBucket(String hash) {
        return redissonClient.getBucket(PREFIX + "token:" + hash);
    }

    private RBucket<RefreshSession> sessionBucket(String sessionId) {
        return redissonClient.getBucket(PREFIX + "session:" + sessionId);
    }

    private RSetCache<String> familyIndex(String familyId) {
        return redissonClient.getSetCache(PREFIX + "family:" + familyId);
    }

    private RSetCache<String> userIndex(Long accountId, String userType, Long userId) {
        return redissonClient.getSetCache(PREFIX + "user:" + accountId + ":" + userType + ":" + userId);
    }

    private RSetCache<String> accountIndex(Long accountId) {
        return redissonClient.getSetCache(PREFIX + "account:" + accountId);
    }

    private Duration remaining(Instant expireTime) {
        return Duration.ofSeconds(remainingSeconds(expireTime));
    }

    private long remainingSeconds(Instant expireTime) {
        return Math.max(1, Duration.between(Instant.now(), expireTime).toSeconds());
    }
}
