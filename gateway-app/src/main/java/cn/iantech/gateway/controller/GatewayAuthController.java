package cn.iantech.gateway.controller;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import cn.iantech.api.model.rbac.AuthenticateRbacAdminReq;
import cn.iantech.api.model.rbac.RbacAdminAuthDTO;
import cn.iantech.common.exception.AppException;
import cn.iantech.common.model.Response;
import cn.iantech.gateway.config.GatewaySecurityProperties;
import cn.iantech.gateway.service.GatewayRbacAuthenticator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iantech.gateway.model.GatewayResponses.success;

/**
 * 权限接口
 */
@RestController
@RequestMapping("/auth")
public class GatewayAuthController {

    private final GatewaySecurityProperties properties;
    private final GatewayRbacAuthenticator authenticator;

    public GatewayAuthController(GatewaySecurityProperties properties, GatewayRbacAuthenticator authenticator) {
        this.properties = properties;
        this.authenticator = authenticator;
    }

    @PostMapping("/login")
    public Response<LoginToken> login(@Valid @RequestBody LoginRequest request) {
        RbacAdminAuthDTO authenticated = authenticator.authenticate(AuthenticateRbacAdminReq.builder()
                .tenantId(properties.tenantId())
                .username(request.username())
                .password(request.password())
                .build());
        if (authenticated == null || authenticated.getRoleCodes() == null
                || !authenticated.getRoleCodes().contains("RBAC_ADMIN")) {
            throw new AppException("AUTH_REQUIRED", "账号或密码错误");
        }
        StpUtil.login(authenticated.getUsername());
        StpUtil.getTokenSession().set(SaSession.ROLE_LIST, authenticated.getRoleCodes());
        return success(new LoginToken(StpUtil.getTokenValue()));
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record LoginToken(String token) {
    }
}
