package ${package}.config;

import cn.iantech.api.model.auth.AuthIdentityDTO;
import cn.iantech.common.constant.Constants;
import cn.iantech.common.exception.AppException;
import ${package}.service.GatewayAuthClient;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayAuthFilterTest {

    @Test
    void shouldDelegateMissingTokenToHandlerResolver() throws Exception {
        AtomicReference<RuntimeException> captured = new AtomicReference<>();
        GatewayAuthFilter filter = new GatewayAuthFilter(new StubAuthClient(null), resolver(captured));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/rbac/roles");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            throw new AssertionError("认证失败时不应继续调用下游");
        });

        assertEquals(Constants.ResponseCode.AUTH_REQUIRED.getCode(), ((AppException) captured.get()).getCode());
        assertEquals(401, response.getStatus());
    }

    @Test
    void shouldEstablishTrustedContextForValidToken() throws Exception {
        AuthIdentityDTO identity = AuthIdentityDTO.builder()
                .accountId(1001L).userId(2001L).username("root")
                .userType("ROOT_ACCOUNT").sessionId("session-1").build();
        GatewayAuthFilter filter = new GatewayAuthFilter(new StubAuthClient(identity), resolver(new AtomicReference<>()));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/status");
        request.addHeader("Authorization", "Bearer opaque-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<AuthIdentityDTO> forwarded = new AtomicReference<>();
        FilterChain chain = (servletRequest, servletResponse) ->
                forwarded.set(GatewayAuthFilter.identity((MockHttpServletRequest) servletRequest));

        filter.doFilter(request, response, chain);

        assertEquals(identity, forwarded.get());
        assertEquals("opaque-token", GatewayAuthFilter.accessToken(request));
        assertEquals(200, response.getStatus());
        assertTrue(response.getHeader("X-Request-Id") != null);
    }

    @Test
    void shouldRethrowWhenExceptionResolverReturnsNull() {
        GatewayAuthFilter filter = new GatewayAuthFilter(new StubAuthClient(null),
                (request, response, handler, exception) -> null);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/rbac/roles");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThrows(AppException.class, () -> filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            throw new AssertionError("认证失败时不应继续调用下游");
        }));
    }

    private HandlerExceptionResolver resolver(AtomicReference<RuntimeException> captured) {
        return (request, response, handler, exception) -> {
            captured.set(exception instanceof RuntimeException runtimeException
                    ? runtimeException : new RuntimeException(exception));
            response.setStatus(exception instanceof AppException appException
                    && Constants.ResponseCode.AUTH_REQUIRED.getCode().equals(appException.getCode()) ? 401 : 503);
            return new ModelAndView();
        };
    }

    static class StubAuthClient extends GatewayAuthClient {

        private final AuthIdentityDTO identity;

        StubAuthClient(AuthIdentityDTO identity) {
            this.identity = identity;
        }

        @Override
        public AuthIdentityDTO validate(String accessToken) {
            return identity;
        }
    }
}
