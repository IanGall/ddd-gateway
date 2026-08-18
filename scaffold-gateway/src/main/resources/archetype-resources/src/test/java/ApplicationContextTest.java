package ${package};

import cn.iantech.api.model.auth.AuthIdentityDTO;
import cn.iantech.api.model.auth.AuthTokenDTO;
import ${package}.service.GatewayAuthClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "dubbo.registry.address=N/A",
        "dubbo.config-center.address=N/A",
        "dubbo.consumer.init=false",
        "gateway.security.platform.token=test-platform-token"
})
class ApplicationContextTest {

    private static final String ACCESS_TOKEN = "opaque-access-token";

    @LocalServerPort
    private int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    void shouldLoadGatewayApplicationContext() {
    }

    @Test
    void healthCheckShouldAllowAnonymousAccess() throws IOException, InterruptedException {
        HttpResponse<String> response = get("/actuator/health", null);

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("UP"));
    }

    @Test
    void businessEndpointShouldRejectAnonymousAccess() throws IOException, InterruptedException {
        assertEquals(401, get("/api/status", null).statusCode());
    }

    @Test
    void validOpaqueTokenShouldAllowBusinessRequest() throws IOException, InterruptedException {
        HttpResponse<String> response = get("/api/status", ACCESS_TOKEN);

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("${rootArtifactId}"));
    }

    @Test
    void authLoginShouldForwardToRbacAuth() throws IOException, InterruptedException {
        HttpResponse<String> response = postJson("/auth/login",
                "{\"loginName\":\"root@1001.com\",\"password\":\"test-password\"}", null);

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains(ACCESS_TOKEN));
    }

    private HttpResponse<String> postJson(String path, String body, String token)
            throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path, String token) throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).GET();
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    @TestConfiguration
    static class AuthClientTestConfiguration {

        @Bean
        @Primary
        GatewayAuthClient authClient() {
            return new FakeGatewayAuthClient();
        }
    }

    static class FakeGatewayAuthClient extends GatewayAuthClient {

        private final AuthIdentityDTO identity = AuthIdentityDTO.builder()
                .accountId(1001L)
                .userId(1001L)
                .username("root")
                .userType("ROOT_ACCOUNT")
                .sessionId("session-1001")
                .build();

        @Override
        public AuthIdentityDTO validate(String accessToken) {
            return ACCESS_TOKEN.equals(accessToken) ? identity : null;
        }

        @Override
        public AuthTokenDTO login(cn.iantech.api.model.auth.AuthLoginReq request) {
            return AuthTokenDTO.builder()
                    .accessToken(ACCESS_TOKEN)
                    .refreshToken("opaque-refresh-token")
                    .tokenType("Bearer")
                    .expiresIn(900)
                    .refreshExpiresIn(2_592_000)
                    .sessionId(identity.getSessionId())
                    .identity(identity)
                    .build();
        }
    }
}
