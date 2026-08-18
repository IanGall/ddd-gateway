package cn.iantech.gateway.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 仅供测试和显式关闭 Redis 的本地环境使用。
 */
public class InMemoryRefreshSessionStore implements RefreshSessionStore {

    private final ConcurrentHashMap<String, RefreshSession> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> tokenIndex = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> familyLocks = new ConcurrentHashMap<>();

    @Override
    public void save(RefreshSession session) {
        sessions.put(session.sessionId(), session);
        tokenIndex.put(session.refreshTokenHash(), session.sessionId());
    }

    @Override
    public Optional<RefreshSession> findByTokenHash(String tokenHash) {
        return Optional.ofNullable(tokenIndex.get(tokenHash)).flatMap(this::findBySessionId);
    }

    @Override
    public Optional<RefreshSession> findBySessionId(String sessionId) {
        RefreshSession session = sessions.get(sessionId);
        if (session != null && !session.expiresAt().isAfter(Instant.now())) {
            sessions.remove(sessionId);
            tokenIndex.remove(session.refreshTokenHash());
            return Optional.empty();
        }
        return Optional.ofNullable(session);
    }

    @Override
    public List<RefreshSession> findByFamilyId(String familyId) {
        return sessions.values().stream()
                .filter(session -> familyId.equals(session.familyId()))
                .filter(session -> session.expiresAt().isAfter(Instant.now()))
                .toList();
    }

    @Override
    public List<RefreshSession> findActiveByUser(Long accountId, Long userId, String userType) {
        Instant now = Instant.now();
        return sessions.values().stream()
                .filter(session -> accountId.equals(session.accountId()))
                .filter(session -> userId.equals(session.userId()))
                .filter(session -> userType.equals(session.userType()))
                .filter(session -> session.isActive(now))
                .toList();
    }

    @Override
    public <T> T withFamilyLock(String familyId, Supplier<T> action) {
        Object lock = familyLocks.computeIfAbsent(familyId, ignored -> new Object());
        synchronized (lock) {
            return action.get();
        }
    }
}
