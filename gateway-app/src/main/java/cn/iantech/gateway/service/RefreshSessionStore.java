package cn.iantech.gateway.service;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * 刷新会话存储。实现必须让同一令牌族的轮换串行执行。
 */
public interface RefreshSessionStore {

    void save(RefreshSession session);

    Optional<RefreshSession> findByTokenHash(String tokenHash);

    Optional<RefreshSession> findBySessionId(String sessionId);

    List<RefreshSession> findByFamilyId(String familyId);

    List<RefreshSession> findActiveByUser(Long accountId, Long userId, String userType);

    <T> T withFamilyLock(String familyId, Supplier<T> action);
}
