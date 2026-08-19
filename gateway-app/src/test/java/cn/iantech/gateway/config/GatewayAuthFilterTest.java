package cn.iantech.gateway.config;

import cn.iantech.api.model.auth.AuthIdentityDTO;
import cn.iantech.common.constant.Constants;
import cn.iantech.common.exception.AppException;
import cn.iantech.context.core.ContextAccessor;
import cn.iantech.context.core.RequestContext;
import cn.iantech.gateway.service.GatewayAuthClient;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

class GatewayAuthFilterTest {

    @Test
    void shouldDelegateMissingTokenToExceptionResolver() throws Exception {
        GatewayAuthClient authClient = mock(GatewayAuthClient.class);
        HandlerExceptionResolver resolver = mock(HandlerExceptionResolver.class);
        GatewayAuthFilter filter = new GatewayAuthFilter(authClient, resolver);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/rbac/roles");
        request.addHeader(GatewayAuthFilter.REQUEST_ID_HEADER, "request-001");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, mock(FilterChain.class));

        verify(resolver).resolveException(any(), any(), isNull(), any(AppException.class));
        assertEquals("/api/rbac/roles", request.getRequestURI());
        assertFalse(response.getContentAsString().contains("AUTH_REQUIRED"));
        assertEquals("request-001", response.getHeader(GatewayAuthFilter.REQUEST_ID_HEADER));
    }

    @Test
    void shouldDelegateAuthUnavailableToExceptionResolver() throws Exception {
        GatewayAuthClient authClient = mock(GatewayAuthClient.class);
        when(authClient.validate("opaque-token"))
                .thenThrow(new AppException(Constants.ResponseCode.AUTH_UNAVAILABLE.getCode(),
                        Constants.ResponseCode.AUTH_UNAVAILABLE.getInfo()));
        HandlerExceptionResolver resolver = mock(HandlerExceptionResolver.class);
        GatewayAuthFilter filter = new GatewayAuthFilter(authClient, resolver);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/rbac/roles");
        request.addHeader("Authorization", "Bearer opaque-token");

        filter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));

        verify(resolver).resolveException(any(), any(), isNull(), any(AppException.class));
    }

    @Test
    void shouldBuildTrustedContextBeforeCallingChain() throws Exception {
        GatewayAuthClient authClient = mock(GatewayAuthClient.class);
        when(authClient.validate("opaque-token")).thenReturn(AuthIdentityDTO.builder()
                .accountId(100L).userId(200L).username("operator").userType("SUB_ACCOUNT")
                .sessionId("session-1").build());
        HandlerExceptionResolver resolver = mock(HandlerExceptionResolver.class);
        GatewayAuthFilter filter = new GatewayAuthFilter(authClient, resolver);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/rbac/roles");
        request.addHeader("Authorization", "Bearer opaque-token");
        AtomicReference<RequestContext> captured = new AtomicReference<>();
        FilterChain chain = (servletRequest, response) -> captured.set(ContextAccessor.current().orElseThrow());

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertEquals("operator", captured.get().principalName());
        assertEquals("100", captured.get().tenantId());
        assertEquals("200", captured.get().userId());
        assertEquals("gateway", captured.get().source());
        assertFalse(ContextAccessor.current().isPresent());
    }
}
