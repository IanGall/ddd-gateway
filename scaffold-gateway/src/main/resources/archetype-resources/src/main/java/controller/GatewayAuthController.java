package ${package}.controller;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import cn.iantech.api.model.rbac.AuthenticateRbacReq;
import cn.iantech.api.model.rbac.RbacAuthDTO;
import cn.iantech.common.exception.AppException;
import cn.iantech.common.model.Response;
import ${package}.config.GatewaySessionKeys;
import ${package}.service.GatewayRbacAuthenticator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static ${package}.model.GatewayResponses.success;

/** 生成网关的主账号与子账号登录接口。 */
@RestController
@RequestMapping("/auth")
public class GatewayAuthController {

    private final GatewayRbacAuthenticator authenticator;

    public GatewayAuthController(GatewayRbacAuthenticator authenticator) {
        this.authenticator = authenticator;
    }

    @PostMapping("/login")
    public Response<LoginToken> login(@Valid @RequestBody LoginRequest request) {
        RbacAuthDTO authenticated = authenticator.authenticate(AuthenticateRbacReq.builder()
                .loginName(request.loginName())
                .password(request.password())
                .build());
        if (authenticated == null) {
            throw new AppException("AUTH_REQUIRED", "账号或密码错误");
        }
        StpUtil.login(authenticated.getUserType() + ":" + authenticated.getUserId());
        StpUtil.getTokenSession()
                .set(GatewaySessionKeys.ACCOUNT_ID, authenticated.getAccountId().toString())
                .set(GatewaySessionKeys.USER_ID, authenticated.getUserId().toString())
                .set(GatewaySessionKeys.USERNAME, authenticated.getUsername())
                .set(GatewaySessionKeys.USER_TYPE, authenticated.getUserType())
                .set(SaSession.ROLE_LIST, authenticated.getRoleCodes())
                .set(GatewaySessionKeys.PERMISSION_LIST, authenticated.getPermissionCodes());
        return success(new LoginToken(StpUtil.getTokenValue()));
    }

    public record LoginRequest(@NotBlank String loginName, @NotBlank String password) {
    }

    public record LoginToken(String token) {
    }
}
