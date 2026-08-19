package ${package}.config;

import cn.iantech.api.model.auth.AuthIdentityDTO;
import cn.iantech.api.model.channel.ChannelSignatureVerifyReq;
import cn.iantech.common.constant.Constants;
import cn.iantech.common.exception.AppException;
import cn.iantech.context.core.ContextAccessor;
import cn.iantech.context.core.ContextScope;
import cn.iantech.context.core.ContextValidator;
import cn.iantech.context.core.RequestContext;
import ${package}.service.GatewayAuthClient;
import ${package}.service.GatewayChannelAuthClient;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.util.UUID;

/**
 * 管理端与移动端校验 opaque Bearer Token，渠道业务请求校验 HMAC，并建立可信请求上下文。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class GatewayAuthFilter extends OncePerRequestFilter {

    public static final String IDENTITY_ATTRIBUTE = GatewayAuthFilter.class.getName() + ".identity";
    public static final String ACCESS_TOKEN_ATTRIBUTE = GatewayAuthFilter.class.getName() + ".accessToken";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    private final GatewayAuthClient authClient;
    private final GatewayChannelAuthClient channelAuthClient;
    private final HandlerExceptionResolver handlerExceptionResolver;

    public GatewayAuthFilter(GatewayAuthClient authClient,
                             @Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver) {
        this(authClient, null, handlerExceptionResolver);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public GatewayAuthFilter(GatewayAuthClient authClient, GatewayChannelAuthClient channelAuthClient,
                             @Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver) {
        this.authClient = authClient;
        this.channelAuthClient = channelAuthClient;
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    public static String accessToken(HttpServletRequest request) {
        Object value = request.getAttribute(ACCESS_TOKEN_ATTRIBUTE);
        return value == null ? null : value.toString();
    }

    public static AuthIdentityDTO identity(HttpServletRequest request) {
        Object value = request.getAttribute(IDENTITY_ATTRIBUTE);
        return value instanceof AuthIdentityDTO identity ? identity : null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestId = validOrGenerate(request.getHeader(REQUEST_ID_HEADER));
        response.setHeader(REQUEST_ID_HEADER, requestId);
        if (!requiresAuthentication(request)) {
            try (ContextScope ignored = ContextAccessor.open(new RequestContext(requestId, null, null, null,
                    null, null, null, "gateway", null))) {
                filterChain.doFilter(request, response);
            }
            return;
        }

        if (isIntegrationRequest(request)) {
            authenticateIntegration(request, response, filterChain, requestId);
            return;
        }

        String accessToken = bearerToken(request.getHeader("Authorization"));
        if (accessToken == null) {
            resolveException(request, response, authRequired("需要认证"));
            return;
        }

        AuthIdentityDTO identity;
        try {
            identity = authClient.validate(accessToken);
        } catch (RuntimeException exception) {
            resolveException(request, response, exception);
            return;
        }
        normalizeIdentity(identity);
        if (!validIdentity(identity)) {
            resolveException(request, response, authRequired("认证令牌无效或已过期"));
            return;
        }

        request.setAttribute(IDENTITY_ATTRIBUTE, identity);
        request.setAttribute(ACCESS_TOKEN_ATTRIBUTE, accessToken);
        continueWithIdentity(request, response, filterChain, requestId, identity);
    }

    private void authenticateIntegration(HttpServletRequest request, HttpServletResponse response,
                                         FilterChain filterChain, String requestId) throws IOException, ServletException {
        if (channelAuthClient == null) {
            resolveException(request, response, new AppException(Constants.ResponseCode.AUTH_UNAVAILABLE.getCode(),
                    Constants.ResponseCode.AUTH_UNAVAILABLE.getInfo()));
            return;
        }
        try {
            CachedBodyHttpServletRequest wrapped = new CachedBodyHttpServletRequest(request,
                    ChannelCanonicalRequest.MAX_BODY_BYTES);
            ChannelCanonicalRequest.Material material = ChannelCanonicalRequest.create(wrapped, wrapped.body());
            ChannelSignatureVerifyReq rpcRequest = new ChannelSignatureVerifyReq();
            rpcRequest.setChannelCode(material.channelCode());
            rpcRequest.setSecretVersion(material.secretVersion());
            rpcRequest.setTimestamp(material.timestamp());
            rpcRequest.setSignature(material.signature());
            rpcRequest.setCanonicalRequest(material.canonicalRequest());
            AuthIdentityDTO identity = channelAuthClient.authenticate(rpcRequest);
            if (!validIdentity(identity)) {
                resolveException(request, response, authRequired("渠道认证失败"));
                return;
            }
            wrapped.setAttribute(IDENTITY_ATTRIBUTE, identity);
            continueWithIdentity(wrapped, response, filterChain, requestId, identity);
        } catch (CachedBodyHttpServletRequest.ChannelPayloadTooLargeException exception) {
            resolveException(request, response, exception);
        } catch (RuntimeException exception) {
            resolveException(request, response, exception);
        }
    }

    private void continueWithIdentity(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain,
                                      String requestId, AuthIdentityDTO identity) throws IOException, ServletException {
        RequestContext context = new RequestContext(requestId, identity.getUsername(),
                stringValue(identity.getAccountId()), stringValue(identity.getUserId()), identity.getSubjectType(),
                identity.getClientId(), null, "gateway", null, stringValue(identity.getOwnerAccountId()),
                identity.getAuthorizedScope(), stringValue(identity.getCredentialVersion()));
        try (ContextScope ignored = ContextAccessor.open(context)) {
            if (!allowedRoute(request, identity)) {
                resolveException(request, response, new AppException(Constants.ResponseCode.ACCESS_DENIED.getCode(),
                        "主体类型无权访问该资源"));
                return;
            }
            filterChain.doFilter(request, response);
        }
    }

    private boolean requiresAuthentication(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.startsWith("/api/") || path.equals("/auth/logout") || path.equals("/auth/logout-all")
                || path.equals("/auth/sessions") || path.startsWith("/auth/sessions/");
    }

    private boolean isIntegrationRequest(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.equals("/api/integration") || path.startsWith("/api/integration/");
    }

    private String bearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        String token = authorization.substring("Bearer ".length()).trim();
        return token.isBlank() ? null : token;
    }

    private boolean allowedRoute(HttpServletRequest request, AuthIdentityDTO identity) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return switch (identity.getSubjectType()) {
            case "ADMIN_PRIMARY", "ADMIN_SUB_ACCOUNT" -> !path.startsWith("/api/mobile/")
                    && !path.startsWith("/api/integration/");
            case "CUSTOMER" -> path.startsWith("/api/mobile/");
            case "CLIENT" -> (path.equals("/api/integration") || path.startsWith("/api/integration/"))
                    && "CHANNEL_HMAC".equals(identity.getTokenKind());
            default -> false;
        };
    }

    private boolean validIdentity(AuthIdentityDTO identity) {
        if (identity == null || isBlank(identity.getSubjectType()) || isBlank(identity.getSubjectId())
                || isBlank(identity.getTokenKind())) {
            return false;
        }
        return switch (identity.getSubjectType()) {
            case "ADMIN_PRIMARY", "ADMIN_SUB_ACCOUNT" ->
                    identity.getAccountId() != null && identity.getUserId() != null && !isBlank(identity.getSessionId());
            case "CUSTOMER" -> identity.getUserId() != null && !isBlank(identity.getSessionId());
            case "CLIENT" -> !isBlank(identity.getClientId()) && "CHANNEL_HMAC".equals(identity.getTokenKind())
                    && identity.getOwnerAccountId() != null && identity.getCredentialVersion() != null
                    && "integration:access".equals(identity.getAuthorizedScope());
            default -> false;
        };
    }

    private void normalizeIdentity(AuthIdentityDTO identity) {
        if (identity == null) return;
        if (isBlank(identity.getSubjectType()) && identity.getUserType() != null) {
            identity.setSubjectType("PRIMARY".equals(identity.getUserType()) ? "ADMIN_PRIMARY" : "ADMIN_SUB_ACCOUNT");
        }
        if (isBlank(identity.getSubjectId()) && identity.getUserId() != null)
            identity.setSubjectId(String.valueOf(identity.getUserId()));
        if (isBlank(identity.getTokenKind())) identity.setTokenKind("OPAQUE");
    }

    private String stringValue(Long value) {
        return value == null ? null : String.valueOf(value);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String validOrGenerate(String value) {
        String validated = ContextValidator.validOrNull(value);
        return validated == null ? UUID.randomUUID().toString() : validated;
    }

    private AppException authRequired(String info) {
        return new AppException(Constants.ResponseCode.AUTH_REQUIRED.getCode(), info);
    }

    private void resolveException(HttpServletRequest request, HttpServletResponse response,
                                  RuntimeException exception) {
        ModelAndView resolved = handlerExceptionResolver.resolveException(request, response, null, exception);
        if (resolved == null) {
            throw exception;
        }
    }
}
