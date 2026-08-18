package ${package}.service;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import cn.iantech.api.model.rbac.RbacAuthDTO;
import cn.iantech.api.model.rbac.ReloadRbacAuthReq;
import cn.iantech.common.exception.AppException;
import ${package}.config.GatewaySessionKeys;
import ${package}.config.GatewayTokenProperties;
import ${package}.model.AuthSessionView;
import ${package}.model.AuthTokenResponse;
import ${package}.model.RefreshSession;
import ${package}.model.RefreshTokenRecord;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** 双令牌签发、轮换、重放检测和设备会话撤销。 */
@Service
public class GatewayTokenService {

    private static final String TOKEN_TYPE = "Bearer";
    private static final int REFRESH_TOKEN_BYTES = 32;

    private final GatewayRbacAuthenticator authenticator;
    private final RefreshSessionStore store;
    private final GatewayTokenProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public GatewayTokenService(GatewayRbacAuthenticator authenticator,
                               RefreshSessionStore store,
                               GatewayTokenProperties properties) {
        this.authenticator = authenticator;
        this.store = store;
        this.properties = properties;
    }

    public AuthTokenResponse login(RbacAuthDTO identity, RefreshSession.ClientMetadata metadata) {
        Instant now = Instant.now();
        Instant expireTime = now.plus(properties.getRefreshTtl());
        String refreshToken = newRefreshToken();
        String refreshHash = hash(refreshToken);
        String sessionId = UUID.randomUUID().toString();
        String familyId = UUID.randomUUID().toString();
        RefreshSession session = new RefreshSession(sessionId, familyId, identity.getAccountId(),
                identity.getUserId(), identity.getUsername(), identity.getUserType(), refreshHash,
                now, expireTime, null, metadata.clientType(), metadata.deviceId(), metadata.ipAddress(),
                metadata.userAgent());
        RefreshTokenRecord tokenRecord = new RefreshTokenRecord(refreshHash, sessionId, familyId,
                RefreshTokenRecord.Status.ACTIVE, expireTime, null, null);
        store.saveSession(session);
        store.saveToken(tokenRecord);
        store.indexSession(session);
        return issue(identity, sessionId, refreshToken);
    }

    public AuthTokenResponse refresh(String refreshToken, RefreshSession.ClientMetadata metadata) {
        String tokenHash = hash(refreshToken);
        RefreshTokenRecord initial = requireToken(tokenHash);
        return store.withFamilyLock(initial.familyId(), () -> rotate(tokenHash, metadata));
    }

    public void logoutCurrent() {
        StpUtil.checkLogin();
        String sessionId = currentSessionId();
        store.revokeSession(sessionId);
        CurrentIdentity identity = currentIdentity();
        kickAccessTokens(identity.loginId(), sessionId);
    }

    public void logoutAll() {
        CurrentIdentity identity = currentIdentity();
        store.revokeUser(identity.accountId(), identity.userType(), identity.userId());
        StpUtil.getStpLogic().logout(identity.loginId());
    }

    public List<AuthSessionView> sessions() {
        CurrentIdentity identity = currentIdentity();
        String currentSessionId = currentSessionId();
        return store.findUserSessions(identity.accountId(), identity.userType(), identity.userId()).stream()
                .sorted((left, right) -> right.createTime().compareTo(left.createTime()))
                .map(session -> new AuthSessionView(session.sessionId(),
                        session.sessionId().equals(currentSessionId), session.clientType(), session.deviceId(),
                        session.ipAddress(), session.userAgent(), session.createTime(), session.expireTime()))
                .toList();
    }

    public void revokeSession(String sessionId) {
        CurrentIdentity identity = currentIdentity();
        RefreshSession target = store.findSession(sessionId);
        if (target == null || !identity.accountId().equals(target.accountId())
                || !identity.userId().equals(target.userId())
                || !identity.userType().equals(target.userType())) {
            throw new AppException("SESSION_NOT_FOUND", "会话不存在");
        }
        store.revokeSession(sessionId);
        kickAccessTokens(identity.loginId(), sessionId);
    }

    private AuthTokenResponse rotate(String tokenHash, RefreshSession.ClientMetadata metadata) {
        RefreshTokenRecord currentToken = requireToken(tokenHash);
        if (currentToken.status() == RefreshTokenRecord.Status.ROTATED) {
            store.revokeFamily(currentToken.familyId());
            kickSessionAccessTokens(currentToken.sessionId());
            throw new AppException("REFRESH_TOKEN_REUSED", "刷新令牌已被重复使用");
        }
        if (currentToken.status() != RefreshTokenRecord.Status.ACTIVE
                || !currentToken.expireTime().isAfter(Instant.now())) {
            throw new AppException("AUTH_REQUIRED", "刷新令牌无效或已过期");
        }
        RefreshSession session = store.findSession(currentToken.sessionId());
        if (session == null || !session.isActive(Instant.now())
                || !tokenHash.equals(session.currentTokenHash())) {
            store.revokeFamily(currentToken.familyId());
            kickSessionAccessTokens(currentToken.sessionId());
            throw new AppException("AUTH_REQUIRED", "刷新令牌无效或已过期");
        }
        RbacAuthDTO identity = authenticator.reloadAuthentication(ReloadRbacAuthReq.builder()
                .accountId(session.accountId())
                .userId(session.userId())
                .userType(session.userType())
                .build());
        if (!matches(session, identity)) {
            store.revokeFamily(currentToken.familyId());
            kickSessionAccessTokens(session.sessionId());
            throw new AppException("AUTH_REQUIRED", "账号状态已失效");
        }

        String nextToken = newRefreshToken();
        String nextHash = hash(nextToken);
        RefreshTokenRecord nextRecord = new RefreshTokenRecord(nextHash, session.sessionId(), session.familyId(),
                RefreshTokenRecord.Status.ACTIVE, session.expireTime(), null, null);
        RefreshSession nextSession = new RefreshSession(session.sessionId(), session.familyId(),
                identity.getAccountId(), identity.getUserId(), identity.getUsername(), identity.getUserType(),
                nextHash, session.createTime(), session.expireTime(), null, metadata.clientType(),
                metadata.deviceId(), metadata.ipAddress(), metadata.userAgent());
        store.saveToken(nextRecord);
        store.saveToken(currentToken.rotate(nextHash));
        store.saveSession(nextSession);
        return issue(identity, session.sessionId(), nextToken);
    }

    private AuthTokenResponse issue(RbacAuthDTO identity, String sessionId, String refreshToken) {
        String loginId = identity.getUserType() + ":" + identity.getUserId();
        SaLoginParameter parameter = new SaLoginParameter()
                .setTimeout(properties.getAccessTtl().toSeconds())
                .setIsShare(false);
        String accessToken = StpUtil.getStpLogic().createLoginSession(loginId, parameter);
        SaSession tokenSession = StpUtil.getTokenSessionByToken(accessToken);
        tokenSession
                .set(GatewaySessionKeys.ACCOUNT_ID, identity.getAccountId().toString())
                .set(GatewaySessionKeys.USER_ID, identity.getUserId().toString())
                .set(GatewaySessionKeys.USERNAME, identity.getUsername())
                .set(GatewaySessionKeys.USER_TYPE, identity.getUserType())
                .set(GatewaySessionKeys.REFRESH_SESSION_ID, sessionId)
                .set(SaSession.ROLE_LIST, listOrEmpty(identity.getRoleCodes()))
                .set(GatewaySessionKeys.PERMISSION_LIST, listOrEmpty(identity.getPermissionCodes()));
        return new AuthTokenResponse(accessToken, refreshToken, TOKEN_TYPE,
                properties.getAccessTtl().toSeconds(), properties.getRefreshTtl().toSeconds(), sessionId,
                identity.getUserId(), identity.getAccountId(), identity.getUsername(), identity.getUserType(),
                listOrEmpty(identity.getRoleCodes()), listOrEmpty(identity.getPermissionCodes()));
    }

    private RefreshTokenRecord requireToken(String tokenHash) {
        RefreshTokenRecord token = store.findToken(tokenHash);
        if (token == null) {
            throw new AppException("AUTH_REQUIRED", "刷新令牌无效或已过期");
        }
        return token;
    }

    private CurrentIdentity currentIdentity() {
        StpUtil.checkLogin();
        SaSession session = StpUtil.getTokenSession();
        String userType = session.getString(GatewaySessionKeys.USER_TYPE);
        String accountIdValue = session.getString(GatewaySessionKeys.ACCOUNT_ID);
        String userIdValue = session.getString(GatewaySessionKeys.USER_ID);
        if (userType == null || accountIdValue == null || userIdValue == null) {
            throw new AppException("AUTH_REQUIRED", "认证会话无效");
        }
        try {
            Long accountId = Long.valueOf(accountIdValue);
            Long userId = Long.valueOf(userIdValue);
            return new CurrentIdentity(accountId, userType, userId, userType + ":" + userId);
        } catch (NumberFormatException exception) {
            throw new AppException("AUTH_REQUIRED", "认证会话无效");
        }
    }

    private String currentSessionId() {
        String sessionId = StpUtil.getTokenSession().getString(GatewaySessionKeys.REFRESH_SESSION_ID);
        if (sessionId == null || sessionId.isBlank()) {
            throw new AppException("AUTH_REQUIRED", "认证会话无效");
        }
        return sessionId;
    }

    private void kickSessionAccessTokens(String sessionId) {
        RefreshSession session = store.findSession(sessionId);
        if (session != null) {
            kickAccessTokens(session.userType() + ":" + session.userId(), sessionId);
        }
    }

    private void kickAccessTokens(String loginId, String sessionId) {
        StpUtil.getTokenValueListByLoginId(loginId).stream()
                .filter(token -> belongsToSession(token, sessionId))
                .forEach(token -> StpUtil.getStpLogic().kickoutByTokenValue(token));
    }

    private boolean belongsToSession(String accessToken, String sessionId) {
        SaSession tokenSession = StpUtil.getStpLogic().getTokenSessionByToken(accessToken, false);
        return tokenSession != null
                && sessionId.equals(tokenSession.getString(GatewaySessionKeys.REFRESH_SESSION_ID));
    }

    private boolean matches(RefreshSession session, RbacAuthDTO identity) {
        return identity != null
                && Objects.equals(session.accountId(), identity.getAccountId())
                && Objects.equals(session.userId(), identity.getUserId())
                && Objects.equals(session.userType(), identity.getUserType());
    }

    private String newRefreshToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        if (value == null || value.isBlank() || value.length() > 512) {
            throw new AppException("AUTH_REQUIRED", "刷新令牌无效或已过期");
        }
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("JDK 缺少 SHA-256 算法", exception);
        }
    }

    private List<String> listOrEmpty(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private record CurrentIdentity(Long accountId, String userType, Long userId, String loginId) {
    }
}
