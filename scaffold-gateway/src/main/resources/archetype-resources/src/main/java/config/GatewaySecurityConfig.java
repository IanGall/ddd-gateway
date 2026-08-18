package ${package}.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** 注册网关平台级安全配置；认证会话由 RBAC Auth 服务负责。 */
@Configuration
@EnableConfigurationProperties(PlatformSecurityProperties.class)
public class GatewaySecurityConfig {
}
