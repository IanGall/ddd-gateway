package cn.iantech.gateway.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 平台级主账号开户凭据，不进入租户 RBAC。
 */
@Validated
@ConfigurationProperties(prefix = "gateway.security.platform")
public record PlatformSecurityProperties(@NotBlank String token) {
}
