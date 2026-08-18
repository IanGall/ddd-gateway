package cn.iantech.gateway.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.filter.SaServletFilter;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Sa-Token 网关路由鉴权配置。
 */
@Configuration
@EnableConfigurationProperties(GatewaySecurityProperties.class)
public class SaTokenConfig {

    public static final String RBAC_ADMIN_ROLE = "RBAC_ADMIN";

    @Bean
    SaServletFilter saServletFilter() {
        return new SaServletFilter()
                .addInclude("/**")
                .addExclude("/actuator/health", "/auth/login")
                .setAuth(request -> SaRouter.match("/api/rbac/**")
                        .check(route -> {
                            StpUtil.checkLogin();
                            StpUtil.checkRole(RBAC_ADMIN_ROLE);
                        }))
                .setError(this::handleAuthError);
    }

    @Bean
    StpInterface stpInterface() {
        return new StpInterface() {
            @Override
            public List<String> getPermissionList(Object loginId, String loginType) {
                return List.of();
            }

            @Override
            public List<String> getRoleList(Object loginId, String loginType) {
                Object roles = StpUtil.getTokenSession().get(SaSession.ROLE_LIST);
                return roles instanceof List<?> roleList
                        ? roleList.stream().filter(String.class::isInstance).map(String.class::cast).toList()
                        : List.of();
            }
        };
    }

    private Object handleAuthError(Throwable exception) {
        boolean notLoggedIn = exception instanceof NotLoginException;
        SaHolder.getResponse()
                .setStatus(notLoggedIn ? 401 : 403)
                .setHeader("Content-Type", "application/json;charset=UTF-8");
        return notLoggedIn
                ? "{\"code\":\"AUTH_REQUIRED\",\"info\":\"需要认证\",\"data\":null}"
                : "{\"code\":\"ACCESS_DENIED\",\"info\":\"无权访问\",\"data\":null}";
    }
}
