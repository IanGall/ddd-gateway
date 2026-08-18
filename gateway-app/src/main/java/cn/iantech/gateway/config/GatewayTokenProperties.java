package cn.iantech.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 网关访问令牌、刷新令牌与共享会话配置。
 */
@ConfigurationProperties(prefix = "gateway.security.session")
public class GatewayTokenProperties {

    private final Redis redis = new Redis();
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

    public Redis getRedis() {
        return redis;
    }

    public static final class Redis {

        private String address;
        private String password;
        private int database;

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public int getDatabase() {
            return database;
        }

        public void setDatabase(int database) {
            this.database = database;
        }
    }
}
