package ${package};

import cn.iantech.api.model.rbac.RbacAuthDTO;
import ${package}.service.GatewayRbacAuthenticator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "dubbo.registry.address=N/A",
        "dubbo.config-center.address=N/A",
        "dubbo.consumer.init=false",
        "gateway.security.platform.token=test-platform-token"
})
class ApplicationContextTest {

    @MockitoBean
    private GatewayRbacAuthenticator authenticator;

    @org.junit.jupiter.api.BeforeEach
    void stubAuthentication() {
        org.mockito.Mockito.when(authenticator.authenticate(org.mockito.ArgumentMatchers.any()))
                .thenReturn(RbacAuthDTO.builder().userId(1001L).accountId(1001L).username("root")
                        .userType("ROOT_ACCOUNT").roleCodes(java.util.List.of())
                        .permissionCodes(java.util.List.of("*")).build());
    }

    @LocalServerPort
    private int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    // 验证生成的网关应用上下文能够成功加载
    @Test
    void shouldLoadGatewayApplicationContext() {
    }

    // 验证健康检查允许匿名访问
    @Test
    void healthCheckShouldAllowAnonymousAccess() throws IOException, InterruptedException {
        HttpResponse<String> response = get("/actuator/health", null);

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("UP"));
    }

    // 验证业务接口拒绝匿名访问
    @Test
    void businessEndpointShouldRejectAnonymousAccess() throws IOException, InterruptedException {
        assertEquals(401, get("/api/status", null).statusCode());
    }

    // 验证主账号能够访问业务接口
    @Test
    void accountShouldAccessBusinessEndpoint() throws IOException, InterruptedException {
        HttpRequest loginRequest = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"loginName\":\"root@1001.com\",\"password\":\"test-password\"}"))
                .build();
        HttpResponse<String> loginResponse = httpClient.send(loginRequest, HttpResponse.BodyHandlers.ofString());
        String token = Pattern.compile("\\\"token\\\":\\\"([^\\\"]+)\\\"")
                .matcher(loginResponse.body()).results().findFirst().orElseThrow().group(1);
        HttpResponse<String> response = get("/api/status", token);

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("${rootArtifactId}"));
    }

    private HttpResponse<String> get(String path, String token) throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).GET();
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
