package ${package}.service;

import ${package}.model.RefreshSession;
import ${package}.model.RefreshTokenRecord;

import java.util.List;
import java.util.function.Supplier;

/** Refresh Session 的共享存储边界。 */
public interface RefreshSessionStore {

    RefreshTokenRecord findToken(String tokenHash);

    RefreshSession findSession(String sessionId);

    void saveToken(RefreshTokenRecord token);

    void saveSession(RefreshSession session);

    void indexSession(RefreshSession session);

    List<RefreshSession> findUserSessions(Long accountId, String userType, Long userId);

    void revokeSession(String sessionId);

    void revokeFamily(String familyId);

    void revokeUser(Long accountId, String userType, Long userId);

    void revokeAccount(Long accountId);

    <T> T withFamilyLock(String familyId, Supplier<T> operation);
}
