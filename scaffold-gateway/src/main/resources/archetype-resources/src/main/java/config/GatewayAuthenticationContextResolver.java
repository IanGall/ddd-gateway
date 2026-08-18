package ${package}.config;

import cn.iantech.context.web.AuthenticationContextResolver;
import cn.iantech.context.web.ResolvedAuthenticationContext;
import org.springframework.stereotype.Component;

/** 将生成网关的已认证管理员映射为配置中的可信租户上下文。 */
@Component
public final class GatewayAuthenticationContextResolver implements AuthenticationContextResolver {

    private final GatewaySecurityProperties properties;

    public GatewayAuthenticationContextResolver(GatewaySecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    public ResolvedAuthenticationContext resolve(String loginId) {
        if (loginId == null || loginId.isBlank()) {
            return ResolvedAuthenticationContext.empty();
        }
        return new ResolvedAuthenticationContext(loginId,
                properties.tenantId().toString(), null);
    }
}
