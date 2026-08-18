package cn.iantech.gateway.config;

import cn.iantech.api.model.auth.AuthIdentityDTO;
import cn.iantech.common.exception.AppException;
import cn.iantech.context.core.ContextAccessor;
import cn.iantech.context.core.ContextScope;
import cn.iantech.context.core.ContextValidator;
import cn.iantech.context.core.RequestContext;
import cn.iantech.gateway.service.GatewayAuthClient;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.dubbo.rpc.RpcException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 每个受保护请求通过 RBAC Auth RPC 校验 opaque Bearer Token，并建立可信请求上下文。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class GatewayAuthFilter extends OncePerRequestFilter {

    public static final String IDENTITY_ATTRIBUTE = GatewayAuthFilter.class.getName() + ".identity";
    public static final String ACCESS_TOKEN_ATTRIBUTE = GatewayAuthFilter.class.getName() + ".accessToken";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    private final GatewayAuthClient authClient;

    public GatewayAuthFilter(GatewayAuthClient authClient) {
        this.authClient = authClient;
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
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "AUTH_REQUIRED", "需要认证");
            return;
        }

        AuthIdentityDTO identity;
        try {
            identity = authClient.validate(accessToken);
        } catch (AppException exception) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "AUTH_REQUIRED", "认证令牌无效或已过期");
            return;
        } catch (RpcException exception) {
            writeError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "AUTH_UNAVAILABLE", "认证服务暂不可用");
            return;
        }
        if (!validIdentity(identity)) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "AUTH_REQUIRED", "认证令牌无效或已过期");
            return;
        }

        request.setAttribute(IDENTITY_ATTRIBUTE, identity);
        request.setAttribute(ACCESS_TOKEN_ATTRIBUTE, accessToken);
        RequestContext context = new RequestContext(requestId, identity.getUsername(),
                String.valueOf(identity.getAccountId()), String.valueOf(identity.getUserId()), null, "gateway", null);
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

    private void writeError(HttpServletResponse response, int status, String code, String info) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":\"" + code + "\",\"info\":\"" + info + "\",\"data\":null}");
    }
}
