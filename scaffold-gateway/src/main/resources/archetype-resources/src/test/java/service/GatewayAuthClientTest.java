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
import cn.iantech.api.model.customer.CustomerLoginReq;
import cn.iantech.common.constant.Constants;
import cn.iantech.common.exception.AppException;
import org.apache.dubbo.rpc.RpcException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GatewayAuthClientTest {

    @Test
    void shouldPreserveAppExceptionFromRpcCauseChain() {
        AppException expected = new AppException(Constants.ResponseCode.AUTH_REQUIRED.getCode(), "令牌已过期");
        GatewayAuthClient client = client(new StubAuthService(new IllegalStateException(expected)));

        AppException actual = assertThrows(AppException.class, () -> client.validate("expired-token"));

        assertSame(expected, actual);
    }

    @Test
    void shouldTranslateUndeclaredRpcFailureToAuthUnavailable() {
        GatewayAuthClient client = client(new StubAuthService(new IllegalStateException("connection refused")));

        AppException actual = assertThrows(AppException.class, () -> client.validate("opaque-token"));

        assertEquals(Constants.ResponseCode.AUTH_UNAVAILABLE.getCode(), actual.getCode());
    }

    @Test
    void shouldTranslateRpcTimeoutToGatewayTimeout() {
        GatewayAuthClient client = client(new StubAuthService(
                new RpcException(RpcException.TIMEOUT_EXCEPTION, "timeout")));

        AppException actual = assertThrows(AppException.class, () -> client.validate("opaque-token"));

        assertEquals(Constants.ResponseCode.RPC_TIMEOUT.getCode(), actual.getCode());
        assertEquals(RpcException.class, actual.getCause().getClass());
    }

    private GatewayAuthClient client(IAuthService authService) {
        GatewayAuthClient client = new GatewayAuthClient();
        ReflectionTestUtils.setField(client, "authService", authService);
        return client;
    }

    static class StubAuthService implements IAuthService {

        private final RuntimeException failure;

        StubAuthService(RuntimeException failure) {
            this.failure = failure;
        }

        @Override
        public AuthTokenDTO login(AuthLoginReq req) {
            throw failure;
        }

        @Override
        public AuthTokenDTO customerLogin(CustomerLoginReq req) {
            throw failure;
        }

        @Override
        public AuthTokenDTO refresh(AuthRefreshReq req) {
            throw failure;
        }

        @Override
        public AuthIdentityDTO validate(AuthValidateReq req) {
            throw failure;
        }

        @Override
        public void logout(AuthLogoutReq req) {
            throw failure;
        }

        @Override
        public void logoutAll(AuthLogoutAllReq req) {
            throw failure;
        }

        @Override
        public List<AuthSessionDTO> sessions(AuthSessionQueryReq req) {
            throw failure;
        }

        @Override
        public void revokeSession(AuthRevokeSessionReq req) {
            throw failure;
        }
    }
}
