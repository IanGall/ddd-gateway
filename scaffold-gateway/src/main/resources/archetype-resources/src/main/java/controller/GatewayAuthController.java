package ${package}.controller;

import cn.iantech.api.model.auth.AuthLoginReq;
import cn.iantech.api.model.auth.AuthRefreshReq;
import cn.iantech.api.model.auth.AuthSessionDTO;
import cn.iantech.api.model.auth.AuthTokenDTO;
import cn.iantech.common.model.Response;
import ${package}.config.GatewayAuthFilter;
import ${package}.service.GatewayAuthClient;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static ${package}.model.GatewayResponses.success;

/** Auth HTTP 门面，认证、令牌和会话状态均由 RBAC Auth 服务管理。 */
@RestController
@RequestMapping("/auth")
public class GatewayAuthController {

    private final GatewayAuthClient authClient;

    public GatewayAuthController(GatewayAuthClient authClient) {
        this.authClient = authClient;
    }

    @PostMapping("/login")
    public Response<AuthTokenDTO> login(@Valid @RequestBody LoginRequest request,
                                        HttpServletRequest servletRequest) {
        return success(authClient.login(AuthLoginReq.builder()
                .loginName(request.loginName())
                .password(request.password())
                .clientType(limited(request.clientType(), 64))
                .deviceId(limited(request.deviceId(), 128))
                .ipAddress(limited(servletRequest.getRemoteAddr(), 64))
                .userAgent(limited(servletRequest.getHeader("User-Agent"), 256))
                .build()));
    }

    @PostMapping("/refresh")
    public Response<AuthTokenDTO> refresh(@Valid @RequestBody RefreshRequest request,
                                           HttpServletRequest servletRequest) {
        return success(authClient.refresh(AuthRefreshReq.builder()
                .refreshToken(request.refreshToken())
                .ipAddress(limited(servletRequest.getRemoteAddr(), 64))
                .userAgent(limited(servletRequest.getHeader("User-Agent"), 256))
                .build()));
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
    public Response<Void> revokeSession(@PathVariable @Size(max = 64, message = "会话ID长度不能超过64") String sessionId,
                                        HttpServletRequest request) {
        authClient.revokeSession(requiredAccessToken(request), sessionId);
        return success(null);
    }

    private String requiredAccessToken(HttpServletRequest request) {
        String token = GatewayAuthFilter.accessToken(request);
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("认证过滤器未建立可信令牌上下文");
        }
        return token;
    }

    private String limited(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    public record LoginRequest(
            @NotBlank(message = "登录名不能为空") String loginName,
            @NotBlank(message = "密码不能为空") String password,
            @Size(max = 64, message = "客户端类型长度不能超过64") String clientType,
            @Size(max = 128, message = "设备ID长度不能超过128") String deviceId) {
    }

    public record RefreshRequest(
            @NotBlank(message = "刷新令牌不能为空") @Size(max = 512, message = "刷新令牌长度不能超过512")
            @Pattern(regexp = "[A-Za-z0-9_-]+", message = "刷新令牌格式不正确") String refreshToken) {
    }
}
