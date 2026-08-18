package ${package}.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Access Token 与 Refresh Token 的有效期和并发控制参数。 */
@ConfigurationProperties(prefix = "gateway.security.token")
public class GatewayTokenProperties {

    private Duration accessTtl = Duration.ofMinutes(15);
    private Duration refreshTtl = Duration.ofDays(30);
    private Duration refreshLockWait = Duration.ofSeconds(3);
    private Duration refreshLockLease = Duration.ofSeconds(10);

    public Duration getAccessTtl() {
        return accessTtl;
    }

    public void setAccessTtl(Duration accessTtl) {
        this.accessTtl = accessTtl;
    }

    public Duration getRefreshTtl() {
        return refreshTtl;
    }

    public void setRefreshTtl(Duration refreshTtl) {
        this.refreshTtl = refreshTtl;
    }

    public Duration getRefreshLockWait() {
        return refreshLockWait;
    }

    public void setRefreshLockWait(Duration refreshLockWait) {
        this.refreshLockWait = refreshLockWait;
    }

    public Duration getRefreshLockLease() {
        return refreshLockLease;
    }

    public void setRefreshLockLease(Duration refreshLockLease) {
        this.refreshLockLease = refreshLockLease;
    }
}
