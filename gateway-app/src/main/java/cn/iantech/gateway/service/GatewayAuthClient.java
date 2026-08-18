package cn.iantech.gateway.service;

import cn.iantech.api.IAuthService;
import cn.iantech.api.model.auth.*;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 网关到 RBAC Auth 服务的 RPC 适配器。网关不持有令牌或会话状态。
 */
@Component
public class GatewayAuthClient {

    @DubboReference(version = "1.0.0", protocol = "tri", timeout = 10000, retries = 0, check = false)
    private IAuthService authService;

    public AuthTokenDTO login(AuthLoginReq request) {
        return authService.login(request);
    }

    public AuthTokenDTO refresh(AuthRefreshReq request) {
        return authService.refresh(request);
    }

    public AuthIdentityDTO validate(String accessToken) {
        return authService.validate(AuthValidateReq.builder().accessToken(accessToken).build());
    }

    public void logout(String accessToken) {
        authService.logout(AuthLogoutReq.builder().accessToken(accessToken).build());
    }

    public void logoutAll(String accessToken) {
        authService.logoutAll(AuthLogoutAllReq.builder().accessToken(accessToken).build());
    }

    public List<AuthSessionDTO> sessions(String accessToken) {
        return authService.sessions(AuthSessionQueryReq.builder().accessToken(accessToken).build());
    }

    public void revokeSession(String accessToken, String sessionId) {
        authService.revokeSession(AuthRevokeSessionReq.builder().accessToken(accessToken)
                .sessionId(sessionId).build());
    }
}
