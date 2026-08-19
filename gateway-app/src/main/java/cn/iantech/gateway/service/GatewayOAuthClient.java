package cn.iantech.gateway.service;

import cn.iantech.api.IOAuthService;
import cn.iantech.api.model.oauth.*;
import cn.iantech.gateway.exception.GatewayRpcExceptionTranslator;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

import static cn.iantech.common.constant.Constants.ResponseCode.AUTH_UNAVAILABLE;

@Component
public class GatewayOAuthClient {
    @DubboReference(version = "1.0.0", protocol = "tri", timeout = 10000, retries = 0, check = false)
    private IOAuthService oauthService;

    @Value("${oauth2.client.id:integration-client}")
    private String clientId;
    @Value("${oauth2.client.secret:change-me-in-production}")
    private String clientSecret;

    public String authorize(OAuthAuthorizeReq request) {
        return invoke(() -> oauthService.authorize(request));
    }

    public OAuthTokenDTO token(OAuthTokenReq request) {
        return invoke(() -> oauthService.token(request));
    }

    public OAuthIntrospectDTO introspect(String token) {
        OAuthIntrospectReq request = new OAuthIntrospectReq();
        request.setToken(token);
        request.setClientId(clientId);
        request.setClientSecret(clientSecret);
        return invoke(() -> oauthService.introspect(request));
    }

    public void revoke(String token) {
        OAuthIntrospectReq request = new OAuthIntrospectReq();
        request.setToken(token);
        request.setClientId(clientId);
        request.setClientSecret(clientSecret);
        invoke(() -> oauthService.revoke(request));
    }

    public void revoke(OAuthIntrospectReq request) {
        invoke(() -> oauthService.revoke(request));
    }

    private <T> T invoke(Supplier<T> call) {
        try {
            return call.get();
        } catch (RuntimeException exception) {
            throw GatewayRpcExceptionTranslator.translate(exception, AUTH_UNAVAILABLE);
        }
    }

    private void invoke(Runnable call) {
        try {
            call.run();
        } catch (RuntimeException exception) {
            throw GatewayRpcExceptionTranslator.translate(exception, AUTH_UNAVAILABLE);
        }
    }
}
