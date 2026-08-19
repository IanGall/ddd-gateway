package cn.iantech.gateway.config;

import cn.iantech.api.model.auth.AuthIdentityDTO;
import cn.iantech.common.constant.Constants;
import cn.iantech.common.exception.AppException;
import cn.iantech.context.core.ContextAccessor;
import cn.iantech.context.core.RequestContext;
import cn.iantech.gateway.service.GatewayAuthClient;
import cn.iantech.gateway.service.GatewayChannelAuthClient;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

class GatewayAuthFilterTest {

    @Test
    void shouldDelegateMissingTokenToExceptionResolver() throws Exception {
        GatewayAuthClient authClient = mock(GatewayAuthClient.class);
        HandlerExceptionResolver resolver = mock(HandlerExceptionResolver.class);
        when(resolver.resolveException(any(), any(), isNull(), any())).thenReturn(new ModelAndView());
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
        when(resolver.resolveException(any(), any(), isNull(), any())).thenReturn(new ModelAndView());
        GatewayAuthFilter filter = new GatewayAuthFilter(authClient, resolver);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/rbac/roles");
        request.addHeader("Authorization", "Bearer opaque-token");

        filter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));

        verify(resolver).resolveException(any(), any(), isNull(), any(AppException.class));
    }

    @Test
    void shouldRethrowWhenExceptionResolverReturnsNull() {
        GatewayAuthClient authClient = mock(GatewayAuthClient.class);
        HandlerExceptionResolver resolver = mock(HandlerExceptionResolver.class);
        GatewayAuthFilter filter = new GatewayAuthFilter(authClient, resolver);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/rbac/roles");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThrows(AppException.class, () -> filter.doFilter(request, response, mock(FilterChain.class)));
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

    @Test
    void shouldAuthenticateIntegrationRequestAndKeepBodyReadable() throws Exception {
        GatewayAuthClient authClient = mock(GatewayAuthClient.class);
        GatewayChannelAuthClient channelAuthClient = mock(GatewayChannelAuthClient.class);
        when(channelAuthClient.authenticate(any())).thenReturn(AuthIdentityDTO.builder()
                .subjectType("CLIENT").subjectId("ch_abcdefghijklmnopqrstuv")
                .clientId("ch_abcdefghijklmnopqrstuv").tokenKind("CHANNEL_HMAC")
                .ownerAccountId(100L).credentialVersion(1L).authorizedScope("integration:access").build());
        HandlerExceptionResolver resolver = mock(HandlerExceptionResolver.class);
        GatewayAuthFilter filter = new GatewayAuthFilter(authClient, channelAuthClient, resolver);
        byte[] body = "{\"orderId\":1}".getBytes(StandardCharsets.UTF_8);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/integration/orders");
        request.setContentType("application/json; charset=UTF-8");
        request.setContent(body);
        request.addHeader(ChannelCanonicalRequest.CHANNEL_CODE_HEADER, "ch_abcdefghijklmnopqrstuv");
        request.addHeader(ChannelCanonicalRequest.SECRET_VERSION_HEADER, "1");
        request.addHeader(ChannelCanonicalRequest.TIMESTAMP_HEADER, "1787107200");
        request.addHeader(ChannelCanonicalRequest.CONTENT_SHA256_HEADER,
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body)));
        request.addHeader(ChannelCanonicalRequest.SIGNATURE_HEADER, "1".repeat(64));
        AtomicReference<RequestContext> context = new AtomicReference<>();
        AtomicReference<String> forwardedBody = new AtomicReference<>();
        FilterChain chain = (servletRequest, servletResponse) -> {
            context.set(ContextAccessor.current().orElseThrow());
            forwardedBody.set(new String(servletRequest.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
        };

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        verify(channelAuthClient).authenticate(argThat(rpc -> rpc.getCanonicalRequest().split("\\n", -1).length == 8));
        verifyNoInteractions(authClient);
        assertEquals("{\"orderId\":1}", forwardedBody.get());
        assertEquals("100", context.get().ownerAccountId());
        assertEquals("integration:access", context.get().authorizedScope());
        assertEquals("1", context.get().credentialVersion());
    }
}
