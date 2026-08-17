package cn.iantech.gateway;

import cn.iantech.context.core.ContextAccessor;
import cn.iantech.context.core.RequestContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "dubbo.registry.address=N/A",
                "dubbo.config-center.address=N/A",
                "dubbo.consumer.init=false",
                "gateway.security.admin.username=test-admin",
                "gateway.security.admin.password=test-password"
        })
@Import(ContextPropagationIntegrationTest.ContextController.class)
class ContextPropagationIntegrationTest {

    @LocalServerPort
    private int port;

    // 验证 Basic 认证建立可信上下文，并忽略外部伪造的身份请求头
    @Test
    void shouldBuildTrustedContextFromBasicAuthenticationAndIgnoreForgedIdentityHeaders() throws Exception {
        String credentials = Base64.getEncoder().encodeToString(
                "test-admin:test-password".getBytes(StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/rbac/context"))
                .header("Authorization", "Basic " + credentials)
                .header("X-Request-Id", "request-001")
                .header("X-User-Id", "forged-user")
                .header("X-Tenant-Id", "forged-tenant")
                .GET()
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals("request-001", response.headers().firstValue("X-Request-Id").orElseThrow());
        assertTrue(response.body().contains("\"principalName\":\"test-admin\""));
        assertTrue(response.body().contains("\"source\":\"gateway\""));
        assertTrue(response.body().contains("\"tenantId\":null"));
        assertTrue(response.body().contains("\"userId\":null"));
        assertFalse(ContextAccessor.current().isPresent());
    }

    @RestController
    static class ContextController {

        @GetMapping("/api/rbac/context")
        RequestContext currentContext() {
            return ContextAccessor.current().orElseThrow();
        }
    }
}
