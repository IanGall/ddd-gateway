package ${package}.service;

import cn.iantech.api.IAuthService;
import cn.iantech.api.model.auth.AuthIdentityDTO;
import cn.iantech.api.model.auth.AuthLoginReq;
import cn.iantech.api.model.auth.AuthLogoutAllReq;
import cn.iantech.api.model.auth.AuthLogoutReq;
import cn.iantech.api.model.auth.AuthRefreshReq;
import cn.iantech.api.model.auth.AuthRevokeSessionReq;
import cn.iantech.api.model.auth.AuthSessionDTO;
import cn.iantech.api.model.auth.AuthSessionQueryReq;
import cn.iantech.api.model.auth.AuthTokenDTO;
import cn.iantech.api.model.auth.AuthValidateReq;
import ${package}.exception.GatewayRpcExceptionTranslator;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Supplier;

import static cn.iantech.common.constant.Constants.ResponseCode.AUTH_UNAVAILABLE;

/** 网关到 RBAC Auth 服务的 RPC 适配器，网关不持有令牌或会话状态。 */
@Component
public class GatewayAuthClient {

    @DubboReference(version = "1.0.0", protocol = "tri", timeout = 10000, retries = 0, check = false)
    private IAuthService authService;

    public AuthTokenDTO login(AuthLoginReq request) {
        return invoke(() -> authService.login(request));
    }

    public AuthTokenDTO refresh(AuthRefreshReq request) {
        return invoke(() -> authService.refresh(request));
    }

    public AuthIdentityDTO validate(String accessToken) {
        return invoke(() -> authService.validate(AuthValidateReq.builder().accessToken(accessToken).build()));
    }

    public void logout(String accessToken) {
        invoke(() -> authService.logout(AuthLogoutReq.builder().accessToken(accessToken).build()));
    }

    public void logoutAll(String accessToken) {
        invoke(() -> authService.logoutAll(AuthLogoutAllReq.builder().accessToken(accessToken).build()));
    }

    public List<AuthSessionDTO> sessions(String accessToken) {
        return invoke(() -> authService.sessions(AuthSessionQueryReq.builder().accessToken(accessToken).build()));
    }

    public void revokeSession(String accessToken, String sessionId) {
        invoke(() -> authService.revokeSession(AuthRevokeSessionReq.builder().accessToken(accessToken)
                .sessionId(sessionId).build()));
    }

    private <T> T invoke(Supplier<T> invocation) {
        try {
            return invocation.get();
        } catch (RuntimeException exception) {
            throw GatewayRpcExceptionTranslator.translate(exception, AUTH_UNAVAILABLE);
        }
    }

    private void invoke(Runnable invocation) {
        try {
            invocation.run();
        } catch (RuntimeException exception) {
            throw GatewayRpcExceptionTranslator.translate(exception, AUTH_UNAVAILABLE);
        }
    }
}
