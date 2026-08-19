package ${package}.config;

import cn.iantech.api.model.auth.AuthIdentityDTO;
import cn.iantech.common.constant.Constants;
import cn.iantech.common.exception.AppException;
import cn.iantech.context.core.ContextAccessor;
import cn.iantech.context.core.RequestContext;
import ${package}.service.GatewayAuthClient;
import ${package}.service.GatewayChannelAuthClient;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import java.util.concurrent.atomic.AtomicReference;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

class GatewayAuthFilterTest {

    @Test
    void shouldAllowOnlyMatchingBearerSubjectForAdminAndAppPartitions() throws Exception {
        assertBearerDecision("/api/admin", adminIdentity(), true);
        assertBearerDecision("/api/admin/", adminIdentity(), true);
        assertBearerDecision("/api/admin/rbac/roles", adminIdentity(), true);
        assertBearerDecision("/api/admin/rbac/roles", customerIdentity(), false);
        assertBearerDecision("/api/app", customerIdentity(), true);
        assertBearerDecision("/api/app/", customerIdentity(), true);
        assertBearerDecision("/api/app/orders", customerIdentity(), true);
        assertBearerDecision("/api/app/orders", adminIdentity(), false);
    }

    @Test
    void shouldRejectIdentityWithoutExplicitSubjectContract() throws Exception {
        GatewayAuthClient authClient = mock(GatewayAuthClient.class);
        when(authClient.validate("legacy-token")).thenReturn(AuthIdentityDTO.builder()
                .accountId(100L).userId(200L).username("operator").userType("SUB_ACCOUNT")
                .sessionId("session-1").build());
        HandlerExceptionResolver resolver = resolvedExceptionResolver();
        GatewayAuthFilter filter = new GatewayAuthFilter(authClient, resolver);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(bearerRequest("/api/admin/rbac/roles", "legacy-token"),
                new MockHttpServletResponse(), chain);

        verifyNoInteractions(chain);
        verify(resolver).resolveException(any(), any(), isNull(),
                argThat(exception -> exception instanceof AppException appException
                        && "AUTH_REQUIRED".equals(appException.getCode())));
    }

    @Test
    void shouldRejectWrongMethodAndConfusablePrefixes() throws Exception {
        GatewayAuthClient authClient = mock(GatewayAuthClient.class);
        HandlerExceptionResolver resolver = resolvedExceptionResolver();
        GatewayAuthFilter filter = new GatewayAuthFilter(authClient, resolver);

        for (String path : new String[]{"/api/admin/auth/login", "/api/admin/auth/refresh",
                "/api/app/auth/register", "/api/app/auth/login", "/api/app/auth/refresh",
                "/api/admin/platform/accounts", "/api/adminx", "/api/application", "/api/external-test",
                "/auth/login", "/customer/register", "/platform/accounts", "/api/rbac/roles",
                "/api/mobile/orders", "/api/integration/orders", "/api/unknown", "/api/admin//roles",
                "/api/admin/roles;version=1", "/api/admin/./roles", "/api/admin/../app",
                "/api/admin%2froles", "/api/admin\\roles"}) {
            FilterChain chain = mock(FilterChain.class);
            filter.doFilter(new MockHttpServletRequest("GET", path), new MockHttpServletResponse(), chain);
            verifyNoInteractions(chain);
        }
        verifyNoInteractions(authClient);
    }

    @Test
    void shouldDelegateMissingTokenToExceptionResolver() throws Exception {
        GatewayAuthClient authClient = mock(GatewayAuthClient.class);
        HandlerExceptionResolver resolver = mock(HandlerExceptionResolver.class);
        when(resolver.resolveException(any(), any(), isNull(), any())).thenReturn(new ModelAndView());
        GatewayAuthFilter filter = new GatewayAuthFilter(authClient, resolver);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/rbac/roles");
        request.addHeader(GatewayAuthFilter.REQUEST_ID_HEADER, "request-001");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, mock(FilterChain.class));

        verify(resolver).resolveException(any(), any(), isNull(), any(AppException.class));
        assertEquals("/api/admin/rbac/roles", request.getRequestURI());
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
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/rbac/roles");
        request.addHeader("Authorization", "Bearer opaque-token");

        filter.doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));

        verify(resolver).resolveException(any(), any(), isNull(), any(AppException.class));
    }

    @Test
    void shouldRethrowWhenExceptionResolverReturnsNull() {
        GatewayAuthClient authClient = mock(GatewayAuthClient.class);
        HandlerExceptionResolver resolver = mock(HandlerExceptionResolver.class);
        GatewayAuthFilter filter = new GatewayAuthFilter(authClient, resolver);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/rbac/roles");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThrows(AppException.class, () -> filter.doFilter(request, response, mock(FilterChain.class)));
    }

    @Test
    void shouldBuildTrustedContextBeforeCallingChain() throws Exception {
        GatewayAuthClient authClient = mock(GatewayAuthClient.class);
        when(authClient.validate("opaque-token")).thenReturn(AuthIdentityDTO.builder()
                .subjectType("ADMIN_SUB_ACCOUNT").subjectId("200")
                .accountId(100L).userId(200L).username("operator").userType("SUB_ACCOUNT")
                .tokenKind("OPAQUE").sessionId("session-1").build());
        HandlerExceptionResolver resolver = mock(HandlerExceptionResolver.class);
        GatewayAuthFilter filter = new GatewayAuthFilter(authClient, resolver);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/rbac/roles");
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
    void shouldAuthenticateExternalRequestAndKeepBodyReadable() throws Exception {
        GatewayAuthClient authClient = mock(GatewayAuthClient.class);
        GatewayChannelAuthClient channelAuthClient = mock(GatewayChannelAuthClient.class);
        when(channelAuthClient.authenticate(any())).thenReturn(AuthIdentityDTO.builder()
                .subjectType("CLIENT").subjectId("ch_abcdefghijklmnopqrstuv")
                .clientId("ch_abcdefghijklmnopqrstuv").tokenKind("CHANNEL_HMAC")
                .ownerAccountId(100L).credentialVersion(1L).authorizedScope("external:access").build());
        HandlerExceptionResolver resolver = mock(HandlerExceptionResolver.class);
        GatewayAuthFilter filter = new GatewayAuthFilter(authClient, channelAuthClient, resolver);
        byte[] body = "{\"orderId\":1}".getBytes(StandardCharsets.UTF_8);
        MockHttpServletRequest request = signedRequest("POST", "/api/external/orders", body);
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
        assertEquals("external:access", context.get().authorizedScope());
        assertEquals("1", context.get().credentialVersion());
    }

    @Test
    void shouldAcceptExternalRootAndTrailingSlash() throws Exception {
        GatewayAuthClient authClient = mock(GatewayAuthClient.class);
        GatewayChannelAuthClient channelAuthClient = mock(GatewayChannelAuthClient.class);
        when(channelAuthClient.authenticate(any())).thenReturn(AuthIdentityDTO.builder()
                .subjectType("CLIENT").subjectId("ch_abcdefghijklmnopqrstuv")
                .clientId("ch_abcdefghijklmnopqrstuv").tokenKind("CHANNEL_HMAC")
                .ownerAccountId(100L).credentialVersion(1L).authorizedScope("external:access").build());
        GatewayAuthFilter filter = new GatewayAuthFilter(authClient, channelAuthClient, resolvedExceptionResolver());

        for (String path : new String[]{"/api/external", "/api/external/"}) {
            FilterChain chain = mock(FilterChain.class);
            filter.doFilter(signedRequest("GET", path, new byte[0]), new MockHttpServletResponse(), chain);
            verify(chain).doFilter(any(), any());
        }
        verifyNoInteractions(authClient);
        verify(channelAuthClient, times(2)).authenticate(any());
    }

    private void assertBearerDecision(String path, AuthIdentityDTO identity, boolean allowed) throws Exception {
        GatewayAuthClient authClient = mock(GatewayAuthClient.class);
        when(authClient.validate("opaque-token")).thenReturn(identity);
        GatewayAuthFilter filter = new GatewayAuthFilter(authClient, resolvedExceptionResolver());
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(bearerRequest(path, "opaque-token"), new MockHttpServletResponse(), chain);

        if (allowed) {
            verify(chain).doFilter(any(), any());
        } else {
            verifyNoInteractions(chain);
        }
    }

    private MockHttpServletRequest bearerRequest(String path, String token) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }

    private MockHttpServletRequest signedRequest(String method, String path, byte[] body) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setContent(body);
        if (body.length > 0) {
            request.setContentType("application/json; charset=UTF-8");
        }
        request.addHeader(ChannelCanonicalRequest.CHANNEL_CODE_HEADER, "ch_abcdefghijklmnopqrstuv");
        request.addHeader(ChannelCanonicalRequest.SECRET_VERSION_HEADER, "1");
        request.addHeader(ChannelCanonicalRequest.TIMESTAMP_HEADER, "1787107200");
        request.addHeader(ChannelCanonicalRequest.CONTENT_SHA256_HEADER,
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body)));
        request.addHeader(ChannelCanonicalRequest.SIGNATURE_HEADER, "1".repeat(64));
        return request;
    }

    private HandlerExceptionResolver resolvedExceptionResolver() {
        HandlerExceptionResolver resolver = mock(HandlerExceptionResolver.class);
        when(resolver.resolveException(any(), any(), isNull(), any())).thenReturn(new ModelAndView());
        return resolver;
    }

    private AuthIdentityDTO adminIdentity() {
        return AuthIdentityDTO.builder().subjectType("ADMIN_SUB_ACCOUNT").subjectId("200")
                .accountId(100L).userId(200L).username("operator").userType("SUB_ACCOUNT")
                .tokenKind("OPAQUE").sessionId("session-1").build();
    }

    private AuthIdentityDTO customerIdentity() {
        return AuthIdentityDTO.builder().subjectType("CUSTOMER").subjectId("300")
                .userId(300L).username("customer").userType("CUSTOMER")
                .tokenKind("OPAQUE").sessionId("session-2").build();
    }
}
