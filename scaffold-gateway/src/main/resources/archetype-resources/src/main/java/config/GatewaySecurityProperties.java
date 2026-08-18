package ${package}.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** 网关只配置数据库认证使用的租户范围。 */
@Validated
@ConfigurationProperties(prefix = "gateway.security.admin")
public record GatewaySecurityProperties(
        @NotNull @Min(1) @Max(4_294_967_295L) Long tenantId) {
}
