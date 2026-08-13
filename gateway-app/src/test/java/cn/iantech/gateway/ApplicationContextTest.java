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
                "gateway.security.admin.username=test-admin",
                "gateway.security.admin.password=test-password"
        })
class ApplicationContextTest {

    @Test
    void 应成功加载SpringBoot4应用上下文() {
    }
}
