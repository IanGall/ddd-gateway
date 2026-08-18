package ${package}.config;

import cn.iantech.context.web.AuthenticationContextResolver;
import cn.iantech.context.web.ResolvedAuthenticationContext;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.stereotype.Component;

/** 从 Sa-Token 可信会话恢复主账号边界和当前用户身份。 */
@Component
public final class GatewayAuthenticationContextResolver implements AuthenticationContextResolver {

    @Override
    public ResolvedAuthenticationContext resolve(String loginId) {
        if (loginId == null || loginId.isBlank()) {
            return ResolvedAuthenticationContext.empty();
        }
        SaSession session = StpUtil.getTokenSession();
        String accountId = session.getString(GatewaySessionKeys.ACCOUNT_ID);
        String userId = session.getString(GatewaySessionKeys.USER_ID);
        String username = session.getString(GatewaySessionKeys.USERNAME);
        if (isBlank(accountId) || isBlank(userId) || isBlank(username)) {
            return ResolvedAuthenticationContext.empty();
        }
        return new ResolvedAuthenticationContext(username, accountId, userId);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
