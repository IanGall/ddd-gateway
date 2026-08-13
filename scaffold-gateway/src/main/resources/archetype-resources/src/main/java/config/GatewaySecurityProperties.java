package ${package}.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** 网关管理员凭据，生产环境必须通过环境变量注入。 */
@Validated
@ConfigurationProperties(prefix = "gateway.security.admin")
public record GatewaySecurityProperties(
        @NotBlank String username,
        @NotBlank @Size(min = 12) String password) {
}
