package ${package};

import cn.iantech.api.model.auth.AuthIdentityDTO;
import cn.iantech.api.model.auth.AuthTokenDTO;
import cn.iantech.api.model.customer.CustomerLoginReq;
import cn.iantech.api.model.customer.CustomerUserDTO;
import ${package}.service.GatewayAuthClient;
import ${package}.service.GatewayCustomerClient;
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
        "dubbo.registry.username=test-user",
        "dubbo.registry.password=test-password",
        "dubbo.config-center.address=N/A",
        "dubbo.consumer.init=false"
})
class ApplicationContextTest {

    private static final String ACCESS_TOKEN = "opaque-access-token";
    private static final String APP_ACCESS_TOKEN = "opaque-app-access-token";

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
        assertEquals(401, get("/api/admin/status", null).statusCode());
    }

    @Test
    void validOpaqueTokenShouldAllowBusinessRequest() throws IOException, InterruptedException {
        HttpResponse<String> response = get("/api/admin/status", ACCESS_TOKEN);

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("${rootArtifactId}"));
    }

    @Test
    void authLoginShouldForwardToRbacAuth() throws IOException, InterruptedException {
        HttpResponse<String> response = postJson("/api/admin/auth/login",
                "{\"loginName\":\"root@1001.com\",\"password\":\"test-password\"}", null);

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains(ACCESS_TOKEN));
    }

    @Test
    void appRegisterAndLoginShouldUseDedicatedRoutes() throws IOException, InterruptedException {
        HttpResponse<String> register = postJson("/api/app/auth/register",
                "{\"mobile\":\"13800138000\",\"password\":\"test-password\",\"displayName\":\"测试用户\"}", null);
        HttpResponse<String> login = postJson("/api/app/auth/login",
                "{\"mobile\":\"13800138000\",\"password\":\"test-password\"}", null);

        assertEquals(200, register.statusCode());
        assertTrue(register.body().contains("13800138000"));
        assertEquals(200, login.statusCode());
        assertTrue(login.body().contains(APP_ACCESS_TOKEN));
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

        @Bean
        @Primary
        GatewayCustomerClient customerClient() {
            return new FakeGatewayCustomerClient();
        }
    }

    static class FakeGatewayAuthClient extends GatewayAuthClient {

        private final AuthIdentityDTO identity = AuthIdentityDTO.builder()
                .accountId(1001L)
                .userId(1001L)
                .username("root")
                .userType("PRIMARY")
                .subjectType("ADMIN_PRIMARY")
                .subjectId("1001")
                .tokenKind("OPAQUE")
                .sessionId("session-1001")
                .build();

        private final AuthIdentityDTO customerIdentity = AuthIdentityDTO.builder()
                .userId(3001L)
                .username("测试用户")
                .userType("CUSTOMER")
                .subjectType("CUSTOMER")
                .subjectId("3001")
                .tokenKind("OPAQUE")
                .sessionId("session-3001")
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

        @Override
        public AuthTokenDTO customerLogin(CustomerLoginReq request) {
            return AuthTokenDTO.builder()
                    .accessToken(APP_ACCESS_TOKEN)
                    .refreshToken("opaque-app-refresh-token")
                    .tokenType("Bearer")
                    .expiresIn(900)
                    .refreshExpiresIn(2_592_000)
                    .sessionId(customerIdentity.getSessionId())
                    .identity(customerIdentity)
                    .build();
        }
    }

    static class FakeGatewayCustomerClient extends GatewayCustomerClient {

        @Override
        public CustomerUserDTO register(String loginName, String password, String displayName) {
            CustomerUserDTO customer = new CustomerUserDTO();
            customer.setId(3001L);
            customer.setLoginName(loginName);
            customer.setDisplayName(displayName);
            customer.setStatus(true);
            return customer;
        }
    }
}
