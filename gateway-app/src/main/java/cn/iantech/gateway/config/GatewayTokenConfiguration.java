package cn.iantech.gateway.config;

import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoForRedisson;
import cn.iantech.gateway.service.InMemoryRefreshSessionStore;
import cn.iantech.gateway.service.RedisRefreshSessionStore;
import cn.iantech.gateway.service.RefreshSessionStore;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 网关认证会话存储配置。
 */
@Configuration
@EnableConfigurationProperties(GatewayTokenProperties.class)
public class GatewayTokenConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "gateway.security.session", name = "redis-enabled", havingValue = "true",
            matchIfMissing = true)
    SaTokenDao saTokenDao(RedissonClient redissonClient, GatewayTokenProperties properties) {
        validate(properties);
        return new SaTokenDaoForRedisson(redissonClient);
    }

    @Bean
    @ConditionalOnProperty(prefix = "gateway.security.session", name = "redis-enabled", havingValue = "true",
            matchIfMissing = true)
    RefreshSessionStore redisRefreshSessionStore(RedissonClient redissonClient) {
        return new RedisRefreshSessionStore(redissonClient);
    }

    @Bean
    @ConditionalOnProperty(prefix = "gateway.security.session", name = "redis-enabled", havingValue = "false",
            matchIfMissing = true)
    RefreshSessionStore inMemoryRefreshSessionStore() {
        return new InMemoryRefreshSessionStore();
    }

    private void validate(GatewayTokenProperties properties) {
        if (properties.getAccessTokenTimeout() <= 0 || properties.getRefreshTokenTimeout() <= 0) {
            throw new IllegalStateException("访问令牌和刷新令牌有效期必须大于零");
        }
    }
}
