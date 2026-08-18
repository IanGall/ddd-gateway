package cn.iantech.gateway.service;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import cn.iantech.api.model.rbac.RbacAuthDTO;
import cn.iantech.api.model.rbac.ReloadRbacAuthReq;
import cn.iantech.common.exception.AppException;
import cn.iantech.gateway.config.GatewaySessionKeys;
import cn.iantech.gateway.config.GatewayTokenProperties;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 网关双令牌签发、轮换与撤销服务。
 */
@Service
public class GatewayTokenService {

    private static final int REFRESH_TOKEN_BYTES = 32;
    private static final String TOKEN_TYPE = "Bearer";

    private final GatewayRbacAuthenticator authenticator;
    private final RefreshSessionStore sessionStore;
    private final GatewayTokenProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public GatewayTokenService(GatewayRbacAuthenticator authenticator, RefreshSessionStore sessionStore,
                               GatewayTokenProperties properties) {
        this.authenticator = authenticator;
        this.sessionStore = sessionStore;
        this.properties = properties;
    }

    public IssuedTokens login(RbacAuthDTO authentication, ClientMetadata metadata) {
        return issue(authentication, normalize(metadata), UUID.randomUUID().toString());
    }

    public IssuedTokens refresh(String refreshToken, ClientMetadata metadata) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw unauthorized("刷新令牌不能为空");
        }
        String tokenHash = hash(refreshToken);
        RefreshSession candidate = sessionStore.findByTokenHash(tokenHash)
                .orElseThrow(() -> unauthorized("刷新令牌无效或已过期"));
        return sessionStore.withFamilyLock(candidate.familyId(),
                () -> rotate(tokenHash, normalize(metadata)));
    }

    public void logoutCurrent(String refreshSessionId) {
        if (refreshSessionId != null) {
            sessionStore.findBySessionId(refreshSessionId).ifPresent(session -> revokeFamily(session.familyId()));
        }
        StpUtil.logout();
    }

    public void logoutAll(Long accountId, Long userId, String userType, String loginId) {
        sessionStore.findActiveByUser(accountId, userId, userType).stream()
                .map(RefreshSession::familyId)
                .distinct()
                .forEach(this::revokeFamily);
        StpUtil.logout(loginId);
    }

    public List<SessionView> sessions(Long accountId, Long userId, String userType, String currentSessionId) {
        return sessionStore.findActiveByUser(accountId, userId, userType).stream()
                .map(session -> new SessionView(session.sessionId(), session.clientType(), session.deviceId(),
                        session.ipAddress(), session.userAgent(), session.createdAt(), session.expiresAt(),
                        session.sessionId().equals(currentSessionId)))
                .toList();
    }

    public void revokeSession(Long accountId, Long userId, String userType, String sessionId) {
        RefreshSession session = sessionStore.findBySessionId(sessionId)
                .orElseThrow(() -> new AppException("ACCESS_DENIED", "会话不存在或无权访问"));
        if (!Objects.equals(accountId, session.accountId()) || !Objects.equals(userId, session.userId())
                || !Objects.equals(userType, session.userType())) {
            throw new AppException("ACCESS_DENIED", "会话不存在或无权访问");
        }
        revokeFamily(session.familyId());
    }

    public void revokeUser(Long accountId, Long userId, String userType) {
        sessionStore.findActiveByUser(accountId, userId, userType).stream()
                .map(RefreshSession::familyId)
                .distinct()
                .forEach(this::revokeFamily);
        StpUtil.logout(loginId(userType, userId));
    }

    private IssuedTokens rotate(String tokenHash, ClientMetadata requestMetadata) {
        RefreshSession current = sessionStore.findByTokenHash(tokenHash)
                .orElseThrow(() -> unauthorized("刷新令牌无效或已过期"));
        Instant now = Instant.now();
        if (!current.isActive(now)) {
            if (current.revokedAt() != null || current.replacedBy() != null) {
                revokeFamily(current.familyId());
                throw unauthorized("检测到刷新令牌重放，当前设备会话已撤销");
            }
            throw unauthorized("刷新令牌无效或已过期");
        }

        RbacAuthDTO authentication = authenticator.reloadAuthentication(ReloadRbacAuthReq.builder()
                .accountId(current.accountId())
                .userId(current.userId())
                .userType(current.userType())
                .build());
        if (authentication == null) {
            revokeFamily(current.familyId());
            throw unauthorized("账号状态已失效，请重新登录");
        }
        if (!Objects.equals(current.accountId(), authentication.getAccountId())
                || !Objects.equals(current.userId(), authentication.getUserId())
                || !Objects.equals(current.userType(), authentication.getUserType())) {
            revokeFamily(current.familyId());
            throw unauthorized("认证身份已变更，请重新登录");
        }

        ClientMetadata metadata = new ClientMetadata(current.clientType(), current.deviceId(),
                requestMetadata.ipAddress(), requestMetadata.userAgent());
        IssuedTokens issued = issue(authentication, metadata, current.familyId());
        sessionStore.save(current.revoke(now, issued.sessionId()));
        StpUtil.logoutByTokenValue(current.accessToken());
        return issued;
    }

    private IssuedTokens issue(RbacAuthDTO authentication, ClientMetadata metadata, String familyId) {
        validate(authentication);
        String sessionId = UUID.randomUUID().toString();
        String refreshToken = newRefreshToken();
        Instant createdAt = Instant.now();
        Instant expiresAt = createdAt.plusSeconds(properties.getRefreshTokenTimeout());
        String accessToken = createAccessToken(authentication, metadata, sessionId);
        RefreshSession session = new RefreshSession(sessionId, familyId, authentication.getAccountId(),
                authentication.getUserId(), authentication.getUserType(), hash(refreshToken), accessToken,
                metadata.clientType(), metadata.deviceId(), metadata.ipAddress(), metadata.userAgent(),
                createdAt, expiresAt, null, null);
        try {
            sessionStore.save(session);
        } catch (RuntimeException exception) {
            StpUtil.logoutByTokenValue(accessToken);
            throw exception;
        }
        return new IssuedTokens(accessToken, refreshToken, TOKEN_TYPE, properties.getAccessTokenTimeout(),
                properties.getRefreshTokenTimeout(), sessionId, authentication);
    }

    private String createAccessToken(RbacAuthDTO authentication, ClientMetadata metadata, String sessionId) {
        SaLoginParameter parameter = SaLoginParameter.create()
                .setTimeout(properties.getAccessTokenTimeout())
                .setIsConcurrent(true)
                .setIsShare(false)
                .setDeviceType(metadata.clientType())
                .setDeviceId(metadata.deviceId());
        String accessToken = StpUtil.createLoginSession(loginId(authentication.getUserType(),
                authentication.getUserId()), parameter);
        SaSession tokenSession = StpUtil.getTokenSessionByToken(accessToken);
        tokenSession
                .set(GatewaySessionKeys.ACCOUNT_ID, authentication.getAccountId().toString())
                .set(GatewaySessionKeys.USER_ID, authentication.getUserId().toString())
                .set(GatewaySessionKeys.USERNAME, authentication.getUsername())
                .set(GatewaySessionKeys.USER_TYPE, authentication.getUserType())
                .set(SaSession.ROLE_LIST, safeList(authentication.getRoleCodes()))
                .set(GatewaySessionKeys.PERMISSION_LIST, safeList(authentication.getPermissionCodes()))
                .set(GatewaySessionKeys.REFRESH_SESSION_ID, sessionId);
        return accessToken;
    }

    private void revokeFamily(String familyId) {
        Instant now = Instant.now();
        sessionStore.withFamilyLock(familyId, () -> {
            sessionStore.findByFamilyId(familyId).stream()
                    .filter(session -> session.isActive(now))
                    .forEach(session -> {
                        sessionStore.save(session.revoke(now, null));
                        StpUtil.logoutByTokenValue(session.accessToken());
                    });
            return null;
        });
    }

    private ClientMetadata normalize(ClientMetadata metadata) {
        String clientType = valueOrDefault(metadata == null ? null : metadata.clientType(), "unknown");
        String deviceId = valueOrDefault(metadata == null ? null : metadata.deviceId(), UUID.randomUUID().toString());
        String ipAddress = valueOrDefault(metadata == null ? null : metadata.ipAddress(), "unknown");
        String userAgent = valueOrDefault(metadata == null ? null : metadata.userAgent(), "unknown");
        return new ClientMetadata(bounded(clientType, 32), bounded(deviceId, 128), bounded(ipAddress, 64),
                bounded(userAgent, 512));
    }

    private String bounded(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private void validate(RbacAuthDTO authentication) {
        if (authentication == null || authentication.getAccountId() == null || authentication.getUserId() == null
                || authentication.getUserType() == null || authentication.getUsername() == null) {
            throw unauthorized("认证身份无效");
        }
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private String newRefreshToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("JVM 不支持 SHA-256", exception);
        }
    }

    private String loginId(String userType, Long userId) {
        return userType + ":" + userId;
    }

    private AppException unauthorized(String message) {
        return new AppException("AUTH_REQUIRED", message);
    }

    public record ClientMetadata(String clientType, String deviceId, String ipAddress, String userAgent) {
    }

    public record IssuedTokens(String accessToken, String refreshToken, String tokenType, long expiresIn,
                               long refreshExpiresIn, String sessionId, RbacAuthDTO authentication) {
    }

    public record SessionView(String sessionId, String clientType, String deviceId, String ipAddress,
                              String userAgent, Instant createdAt, Instant expiresAt, boolean current) {
    }
}
