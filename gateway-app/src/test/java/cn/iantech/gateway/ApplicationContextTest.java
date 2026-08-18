package cn.iantech.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "dubbo.registry.address=N/A",
                "dubbo.registry.username=test-user",
                "dubbo.registry.password=test-password",
                "dubbo.config-center.address=N/A",
                "dubbo.consumer.init=false",
                "gateway.security.session.redis-enabled=false",
                "spring.autoconfigure.exclude=org.redisson.spring.starter.RedissonAutoConfigurationV2,org.redisson.spring.starter.RedissonAutoConfigurationV4",
                "gateway.security.platform.token=test-platform-token"
        })
class ApplicationContextTest {

    // 验证 Spring Boot 4 应用上下文能够成功加载
    @Test
    void shouldLoadSpringBoot4ApplicationContext() {
    }
}
