package cn.iantech.gateway.controller;

import cn.iantech.api.model.auth.*;
import cn.iantech.common.exception.AppException;
import cn.iantech.common.model.Response;
import cn.iantech.gateway.config.GatewayAuthFilter;
import cn.iantech.gateway.service.GatewayAuthClient;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cn.iantech.common.constant.Constants.ResponseCode.AUTH_REQUIRED;
import static cn.iantech.gateway.model.GatewayResponses.success;

/**
 * Auth HTTP 门面。认证状态和令牌均由 RBAC Auth 服务持有。
 */
@RestController
@RequestMapping("/auth")
public class GatewayAuthController {

    private final GatewayAuthClient authClient;

    public GatewayAuthController(GatewayAuthClient authClient) {
        this.authClient = authClient;
    }

    @PostMapping("/login")
    public Response<TokenResponse> login(@Valid @RequestBody LoginRequest request,
                                         HttpServletRequest servletRequest) {
        AuthTokenDTO issued = authClient.login(AuthLoginReq.builder()
                .loginName(request.loginName())
                .password(request.password())
                .clientType(limited(request.clientType(), 32))
                .deviceId(limited(request.deviceId(), 128))
                .ipAddress(limited(servletRequest.getRemoteAddr(), 64))
                .userAgent(limited(servletRequest.getHeader("User-Agent"), 256))
                .build());
        if (issued == null || issued.getIdentity() == null) {
            throw new AppException(AUTH_REQUIRED.getCode(), "账号或密码错误");
        }
        return success(toResponse(issued));
    }

    @PostMapping("/refresh")
    public Response<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request,
                                           HttpServletRequest servletRequest) {
        AuthTokenDTO issued = authClient.refresh(AuthRefreshReq.builder()
                .refreshToken(request.refreshToken())
                .clientType(limited(request.clientType(), 32))
                .deviceId(limited(request.deviceId(), 128))
                .ipAddress(limited(servletRequest.getRemoteAddr(), 64))
                .userAgent(limited(servletRequest.getHeader("User-Agent"), 256))
                .build());
        return success(toResponse(issued));
    }

    @PostMapping("/logout")
    public Response<Void> logout(HttpServletRequest request) {
        authClient.logout(requiredAccessToken(request));
        return success(null);
    }

    @PostMapping("/logout-all")
    public Response<Void> logoutAll(HttpServletRequest request) {
        authClient.logoutAll(requiredAccessToken(request));
        return success(null);
    }

    @GetMapping("/sessions")
    public Response<List<AuthSessionDTO>> sessions(HttpServletRequest request) {
        return success(authClient.sessions(requiredAccessToken(request)));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public Response<Void> revokeSession(
            @Size(max = 64, message = "会话ID长度不能超过64") @PathVariable String sessionId,
            HttpServletRequest request) {
        authClient.revokeSession(requiredAccessToken(request), sessionId);
        return success(null);
    }

    private String requiredAccessToken(HttpServletRequest request) {
        String token = GatewayAuthFilter.accessToken(request);
        if (token == null || token.isBlank()) {
            throw new AppException(AUTH_REQUIRED.getCode(), AUTH_REQUIRED.getInfo());
        }
        return token;
    }

    private String limited(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private TokenResponse toResponse(AuthTokenDTO issued) {
        AuthIdentityDTO identity = issued.getIdentity();
        return new TokenResponse(issued.getAccessToken(), issued.getRefreshToken(), issued.getTokenType(),
                issued.getExpiresIn(), issued.getRefreshExpiresIn(), issued.getSessionId(), identity.getUserId(),
                identity.getAccountId(), identity.getUsername(), identity.getUserType());
    }

    public record LoginRequest(
            @NotBlank(message = "登录名不能为空") String loginName,
            @NotBlank(message = "密码不能为空")
            @Size(min = 8, max = 72, message = "密码长度必须为8到72位") String password,
            @Size(max = 32, message = "客户端类型长度不能超过32") String clientType,
            @Size(max = 128, message = "设备ID长度不能超过128") String deviceId) {
    }

    public record RefreshRequest(
            @NotBlank(message = "刷新令牌不能为空") @Size(max = 256, message = "刷新令牌长度不能超过256")
            @Pattern(regexp = "[A-Za-z0-9_-]+", message = "刷新令牌格式不正确")
            String refreshToken,
            @Size(max = 32, message = "客户端类型长度不能超过32") String clientType,
            @Size(max = 128, message = "设备ID长度不能超过128") String deviceId) {
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
            String userType) {
    }
}
