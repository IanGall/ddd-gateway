package ${package}.config;

import cn.iantech.api.model.auth.AuthIdentityDTO;
import cn.iantech.common.constant.Constants;
import cn.iantech.common.exception.AppException;
import cn.iantech.context.core.ContextAccessor;
import cn.iantech.context.core.ContextScope;
import cn.iantech.context.core.ContextValidator;
import cn.iantech.context.core.RequestContext;
import ${package}.service.GatewayAuthClient;
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

/** 每个受保护请求通过 RBAC Auth RPC 校验 opaque Bearer Token，并建立可信请求上下文。 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class GatewayAuthFilter extends OncePerRequestFilter {

    public static final String IDENTITY_ATTRIBUTE = GatewayAuthFilter.class.getName() + ".identity";
    public static final String ACCESS_TOKEN_ATTRIBUTE = GatewayAuthFilter.class.getName() + ".accessToken";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    private final GatewayAuthClient authClient;
    private final HandlerExceptionResolver handlerExceptionResolver;

    public GatewayAuthFilter(GatewayAuthClient authClient,
                             @Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver) {
        this.authClient = authClient;
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
                    null, "gateway", null))) {
                filterChain.doFilter(request, response);
            }
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
        if (!validIdentity(identity)) {
            resolveException(request, response, authRequired("认证令牌无效或已过期"));
            return;
        }

        request.setAttribute(IDENTITY_ATTRIBUTE, identity);
        request.setAttribute(ACCESS_TOKEN_ATTRIBUTE, accessToken);
        RequestContext context = new RequestContext(requestId, identity.getUsername(),
                String.valueOf(identity.getAccountId()), String.valueOf(identity.getUserId()),
                null, "gateway", null);
        try (ContextScope ignored = ContextAccessor.open(context)) {
            filterChain.doFilter(request, response);
        }
    }

    private boolean requiresAuthentication(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.startsWith("/api/") || path.equals("/auth/logout") || path.equals("/auth/logout-all")
                || path.equals("/auth/sessions") || path.startsWith("/auth/sessions/");
    }

    private String bearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        String token = authorization.substring("Bearer ".length()).trim();
        return token.isBlank() ? null : token;
    }

    private boolean validIdentity(AuthIdentityDTO identity) {
        return identity != null && identity.getAccountId() != null && identity.getUserId() != null
                && !isBlank(identity.getUsername()) && !isBlank(identity.getUserType())
                && !isBlank(identity.getSessionId());
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
