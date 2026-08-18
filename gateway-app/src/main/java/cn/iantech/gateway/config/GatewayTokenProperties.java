package cn.iantech.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 网关访问令牌、刷新令牌与共享会话配置。
 */
@ConfigurationProperties(prefix = "gateway.security.session")
public class GatewayTokenProperties {

    private boolean redisEnabled = true;
    private long accessTokenTimeout = 900;
    private long refreshTokenTimeout = 2_592_000;

    public boolean isRedisEnabled() {
        return redisEnabled;
    }

    public void setRedisEnabled(boolean redisEnabled) {
        this.redisEnabled = redisEnabled;
    }

    public long getAccessTokenTimeout() {
        return accessTokenTimeout;
    }

    public void setAccessTokenTimeout(long accessTokenTimeout) {
        this.accessTokenTimeout = accessTokenTimeout;
    }

    public long getRefreshTokenTimeout() {
        return refreshTokenTimeout;
    }

    public void setRefreshTokenTimeout(long refreshTokenTimeout) {
        this.refreshTokenTimeout = refreshTokenTimeout;
    }

}
