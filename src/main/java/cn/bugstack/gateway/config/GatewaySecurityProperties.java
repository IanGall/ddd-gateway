package cn.bugstack.gateway.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 网关管理员凭据必须由部署环境注入，禁止提供代码默认值。
 */
@Validated
@ConfigurationProperties(prefix = "gateway.security.admin")
public record GatewaySecurityProperties(
        @NotBlank String username,
        @NotBlank @Size(min = 12) String password) {
}
