package cn.iantech.gateway.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.iantech.api.model.rbac.AuthenticateRbacReq;
import cn.iantech.api.model.rbac.RbacAuthDTO;
import cn.iantech.common.exception.AppException;
import cn.iantech.common.model.Response;
import cn.iantech.gateway.config.GatewaySessionKeys;
import cn.iantech.gateway.service.GatewayRbacAuthenticator;
import cn.iantech.gateway.service.GatewayTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cn.iantech.gateway.model.GatewayResponses.success;

/**
 * 登录、令牌刷新和设备会话管理接口。
 */
@RestController
@RequestMapping("/auth")
public class GatewayAuthController {

    private final GatewayRbacAuthenticator authenticator;
    private final GatewayTokenService tokenService;

    public GatewayAuthController(GatewayRbacAuthenticator authenticator, GatewayTokenService tokenService) {
        this.authenticator = authenticator;
        this.tokenService = tokenService;
    }

    @PostMapping("/login")
    public Response<TokenResponse> login(@Valid @RequestBody LoginRequest request,
                                         HttpServletRequest servletRequest) {
        RbacAuthDTO authenticated = authenticator.authenticate(AuthenticateRbacReq.builder()
                .loginName(request.loginName())
                .password(request.password())
                .build());
        if (authenticated == null) {
            throw new AppException("AUTH_REQUIRED", "账号或密码错误");
        }
        GatewayTokenService.IssuedTokens issued = tokenService.login(authenticated,
                metadata(request.clientType(), request.deviceId(), servletRequest));
        return success(toResponse(issued));
    }

    @PostMapping("/refresh")
    public Response<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request,
                                           HttpServletRequest servletRequest) {
        GatewayTokenService.IssuedTokens issued = tokenService.refresh(request.refreshToken(),
                metadata(null, null, servletRequest));
        return success(toResponse(issued));
    }

    @PostMapping("/logout")
    public Response<Void> logout() {
        StpUtil.checkLogin();
        String sessionId = sessionString(GatewaySessionKeys.REFRESH_SESSION_ID);
        tokenService.logoutCurrent(sessionId);
        return success(null);
    }

    @PostMapping("/logout-all")
    public Response<Void> logoutAll() {
        CurrentIdentity identity = currentIdentity();
        tokenService.logoutAll(identity.accountId(), identity.userId(), identity.userType(),
                StpUtil.getLoginIdAsString());
        return success(null);
    }

    @GetMapping("/sessions")
    public Response<List<GatewayTokenService.SessionView>> sessions() {
        CurrentIdentity identity = currentIdentity();
        return success(tokenService.sessions(identity.accountId(), identity.userId(), identity.userType(),
                sessionString(GatewaySessionKeys.REFRESH_SESSION_ID)));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public Response<Void> revokeSession(
            @Size(max = 64, message = "会话ID长度不能超过64") @PathVariable String sessionId) {
        CurrentIdentity identity = currentIdentity();
        tokenService.revokeSession(identity.accountId(), identity.userId(), identity.userType(), sessionId);
        return success(null);
    }

    private CurrentIdentity currentIdentity() {
        StpUtil.checkLogin();
        try {
            return new CurrentIdentity(Long.valueOf(sessionString(GatewaySessionKeys.ACCOUNT_ID)),
                    Long.valueOf(sessionString(GatewaySessionKeys.USER_ID)),
                    sessionString(GatewaySessionKeys.USER_TYPE));
        } catch (RuntimeException exception) {
            throw new AppException("AUTH_REQUIRED", "认证会话身份无效");
        }
    }

    private String sessionString(String key) {
        Object value = StpUtil.getTokenSession().get(key);
        return value == null ? null : value.toString();
    }

    private GatewayTokenService.ClientMetadata metadata(String clientType, String deviceId,
                                                        HttpServletRequest request) {
        return new GatewayTokenService.ClientMetadata(limited(clientType, 32), limited(deviceId, 128),
                limited(request.getRemoteAddr(), 64), limited(request.getHeader("User-Agent"), 256));
    }

    private String limited(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private TokenResponse toResponse(GatewayTokenService.IssuedTokens issued) {
        RbacAuthDTO authentication = issued.authentication();
        return new TokenResponse(issued.accessToken(), issued.refreshToken(), issued.tokenType(),
                issued.expiresIn(), issued.refreshExpiresIn(), issued.sessionId(), authentication.getUserId(),
                authentication.getAccountId(), authentication.getUsername(), authentication.getUserType(),
                safeList(authentication.getRoleCodes()), safeList(authentication.getPermissionCodes()));
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    public record LoginRequest(
            @NotBlank(message = "登录名不能为空") String loginName,
            @NotBlank(message = "密码不能为空") String password,
            @Size(max = 32, message = "客户端类型长度不能超过32") String clientType,
            @Size(max = 128, message = "设备ID长度不能超过128") String deviceId) {
    }

    public record RefreshRequest(
            @NotBlank(message = "刷新令牌不能为空") @Size(max = 64, message = "刷新令牌长度不能超过64")
            @Pattern(regexp = "[A-Za-z0-9_-]+", message = "刷新令牌格式不正确")
            String refreshToken) {
    }

    public record TokenResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresIn,
            long refreshExpiresIn,
            String sessionId,
            Long userId,
            Long accountId,
            String username,
            String userType,
            List<String> roleCodes,
            List<String> permissionCodes) {
    }

    private record CurrentIdentity(Long accountId, Long userId, String userType) {
    }
}
