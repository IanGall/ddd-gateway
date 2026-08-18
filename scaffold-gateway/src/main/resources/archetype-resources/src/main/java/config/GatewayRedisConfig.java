package ${package}.config;

import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoForRedisson;
import ${package}.service.RedissonRefreshSessionStore;
import ${package}.service.RefreshSessionStore;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 让 Access Token 与 Refresh Session 共用同一个 Redis。 */
@Configuration
@EnableConfigurationProperties(GatewayTokenProperties.class)
public class GatewayRedisConfig {

    @Bean
    @ConditionalOnBean(RedissonClient.class)
    SaTokenDao saTokenDao(RedissonClient redissonClient) {
        return new SaTokenDaoForRedisson(redissonClient);
    }

    @Bean
    @ConditionalOnBean(RedissonClient.class)
    RefreshSessionStore refreshSessionStore(RedissonClient redissonClient, GatewayTokenProperties properties) {
        return new RedissonRefreshSessionStore(redissonClient, properties);
    }
}
