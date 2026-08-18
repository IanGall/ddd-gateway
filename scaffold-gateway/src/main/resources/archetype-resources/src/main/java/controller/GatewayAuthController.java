package ${package}.controller;

import cn.iantech.api.model.rbac.AuthenticateRbacReq;
import cn.iantech.api.model.rbac.RbacAuthDTO;
import cn.iantech.common.exception.AppException;
import cn.iantech.common.model.Response;
import ${package}.model.AuthSessionView;
import ${package}.model.AuthTokenResponse;
import ${package}.model.RefreshSession;
import ${package}.service.GatewayRbacAuthenticator;
import ${package}.service.GatewayTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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

/** 生成网关的主账号与子账号登录接口。 */
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
    public Response<AuthTokenResponse> login(@Valid @RequestBody LoginRequest request,
                                             HttpServletRequest servletRequest) {
        RbacAuthDTO authenticated = authenticator.authenticate(AuthenticateRbacReq.builder()
                .loginName(request.loginName())
                .password(request.password())
                .build());
        if (authenticated == null) {
            throw new AppException("AUTH_REQUIRED", "账号或密码错误");
        }
        return success(tokenService.login(authenticated, metadata(servletRequest)));
    }

    @PostMapping("/refresh")
    public Response<AuthTokenResponse> refresh(@Valid @RequestBody RefreshRequest request,
                                               HttpServletRequest servletRequest) {
        return success(tokenService.refresh(request.refreshToken(), metadata(servletRequest)));
    }

    @PostMapping("/logout")
    public Response<Boolean> logout() {
        tokenService.logoutCurrent();
        return success(Boolean.TRUE);
    }

    @PostMapping("/logout-all")
    public Response<Boolean> logoutAll() {
        tokenService.logoutAll();
        return success(Boolean.TRUE);
    }

    @GetMapping("/sessions")
    public Response<List<AuthSessionView>> sessions() {
        return success(tokenService.sessions());
    }

    @DeleteMapping("/sessions/{sessionId}")
    public Response<Boolean> revokeSession(@PathVariable String sessionId) {
        tokenService.revokeSession(sessionId);
        return success(Boolean.TRUE);
    }

    private RefreshSession.ClientMetadata metadata(HttpServletRequest request) {
        return new RefreshSession.ClientMetadata(
                limited(request.getHeader("X-Client-Type"), 64),
                limited(request.getHeader("X-Device-Id"), 128),
                limited(request.getRemoteAddr(), 64),
                limited(request.getHeader("User-Agent"), 256));
    }

    private String limited(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    public record LoginRequest(@NotBlank String loginName, @NotBlank String password) {
    }

    public record RefreshRequest(@NotBlank @Size(max = 512) String refreshToken) {
    }
}
