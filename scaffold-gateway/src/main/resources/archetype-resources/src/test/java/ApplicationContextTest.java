package ${package};

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "dubbo.registry.address=N/A",
        "dubbo.config-center.address=N/A",
        "dubbo.consumer.init=false",
        "gateway.security.admin.username=test-admin",
        "gateway.security.admin.password=test-password"
})
class ApplicationContextTest {

    @LocalServerPort
    private int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    void 应成功加载网关应用上下文() {
    }

    @Test
    void 健康检查应允许匿名访问() throws IOException, InterruptedException {
        HttpResponse<String> response = get("/actuator/health", null);

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("UP"));
    }

    @Test
    void 业务接口应拒绝匿名访问() throws IOException, InterruptedException {
        assertEquals(401, get("/api/status", null).statusCode());
    }

    @Test
    void 管理员应能访问业务接口() throws IOException, InterruptedException {
        String credentials = Base64.getEncoder()
                .encodeToString("test-admin:test-password".getBytes(StandardCharsets.UTF_8));
        HttpResponse<String> response = get("/api/status", "Basic " + credentials);

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("${rootArtifactId}"));
    }

    private HttpResponse<String> get(String path, String authorization) throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).GET();
        if (authorization != null) {
            request.header("Authorization", authorization);
        }
        return httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
