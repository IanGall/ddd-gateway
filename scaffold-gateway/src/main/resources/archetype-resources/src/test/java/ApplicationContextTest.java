package ${package};

import cn.iantech.api.model.rbac.RbacAuthDTO;
import ${package}.model.RefreshSession;
import ${package}.model.RefreshTokenRecord;
import ${package}.service.GatewayRbacAuthenticator;
import ${package}.service.RefreshSessionStore;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
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
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "dubbo.registry.address=N/A",
        "dubbo.config-center.address=N/A",
        "dubbo.consumer.init=false",
        "gateway.security.platform.token=test-platform-token",
        "spring.autoconfigure.exclude=org.redisson.spring.starter.RedissonAutoConfigurationV2,org.redisson.spring.starter.RedissonAutoConfigurationV4"
})
@Import(ApplicationContextTest.InMemoryRefreshStoreConfig.class)
class ApplicationContextTest {

    @MockitoBean
    private GatewayRbacAuthenticator authenticator;

    @org.junit.jupiter.api.BeforeEach
    void stubAuthentication() {
        RbacAuthDTO identity = RbacAuthDTO.builder().userId(1001L).accountId(1001L).username("root")
                .userType("ROOT_ACCOUNT").roleCodes(java.util.List.of())
                .permissionCodes(java.util.List.of("*")).build();
        org.mockito.Mockito.when(authenticator.authenticate(org.mockito.ArgumentMatchers.any())).thenReturn(identity);
        org.mockito.Mockito.when(authenticator.reloadAuthentication(org.mockito.ArgumentMatchers.any()))
                .thenReturn(identity);
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
        String token = jsonValue(loginResponse.body(), "accessToken");
        HttpResponse<String> response = get("/api/status", token);

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("${rootArtifactId}"));
    }

    // 验证刷新会轮换令牌，重放旧令牌后整族 Access Token 立即失效
    @Test
    void refreshTokenReplayShouldRevokeWholeFamily() throws IOException, InterruptedException {
        HttpResponse<String> login = login();
        String firstRefreshToken = jsonValue(login.body(), "refreshToken");
        HttpResponse<String> refreshed = postJson("/auth/refresh",
                "{\"refreshToken\":\"" + firstRefreshToken + "\"}", null);
        assertEquals(200, refreshed.statusCode());
        String refreshedAccessToken = jsonValue(refreshed.body(), "accessToken");
        String nextRefreshToken = jsonValue(refreshed.body(), "refreshToken");
        assertTrue(!firstRefreshToken.equals(nextRefreshToken));

        assertEquals(401, postJson("/auth/refresh",
                "{\"refreshToken\":\"" + firstRefreshToken + "\"}", null).statusCode());
        assertEquals(401, get("/api/status", refreshedAccessToken).statusCode());
        assertEquals(401, postJson("/auth/refresh",
                "{\"refreshToken\":\"" + nextRefreshToken + "\"}", null).statusCode());
    }

    // 验证设备会话查询与指定会话撤销都会要求并失效 Access Token
    @Test
    void shouldListAndRevokeDeviceSession() throws IOException, InterruptedException {
        HttpResponse<String> login = login();
        String accessToken = jsonValue(login.body(), "accessToken");
        String sessionId = jsonValue(login.body(), "sessionId");

        HttpResponse<String> sessions = get("/auth/sessions", accessToken);
        assertEquals(200, sessions.statusCode());
        assertTrue(sessions.body().contains(sessionId));
        assertEquals(200, delete("/auth/sessions/" + sessionId, accessToken).statusCode());
        assertEquals(401, get("/api/status", accessToken).statusCode());
    }

    private HttpResponse<String> login() throws IOException, InterruptedException {
        return postJson("/auth/login", "{\"loginName\":\"root@1001.com\",\"password\":\"test-password\"}",
                null);
    }

    private String jsonValue(String json, String name) {
        return Pattern.compile("\\\"" + name + "\\\":\\\"([^\\\"]+)\\\"")
                .matcher(json).results().findFirst().orElseThrow().group(1);
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

    private HttpResponse<String> delete(String path, String token) throws IOException, InterruptedException {
        return httpClient.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .header("Authorization", "Bearer " + token).DELETE().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path, String token) throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).GET();
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    @TestConfiguration
    static class InMemoryRefreshStoreConfig {

        @Bean
        RefreshSessionStore refreshSessionStore() {
            return new InMemoryRefreshSessionStore();
        }
    }

    static class InMemoryRefreshSessionStore implements RefreshSessionStore {

        private final Map<String, RefreshTokenRecord> tokens = new ConcurrentHashMap<>();
        private final Map<String, RefreshSession> sessions = new ConcurrentHashMap<>();
        private final Map<String, Object> locks = new ConcurrentHashMap<>();

        @Override
        public RefreshTokenRecord findToken(String tokenHash) {
            return tokens.get(tokenHash);
        }

        @Override
        public RefreshSession findSession(String sessionId) {
            return sessions.get(sessionId);
        }

        @Override
        public void saveToken(RefreshTokenRecord token) {
            tokens.put(token.tokenHash(), token);
        }

        @Override
        public void saveSession(RefreshSession session) {
            sessions.put(session.sessionId(), session);
        }

        @Override
        public void indexSession(RefreshSession session) {
            saveSession(session);
        }

        @Override
        public List<RefreshSession> findUserSessions(Long accountId, String userType, Long userId) {
            return sessions.values().stream()
                    .filter(session -> Objects.equals(accountId, session.accountId()))
                    .filter(session -> Objects.equals(userType, session.userType()))
                    .filter(session -> Objects.equals(userId, session.userId()))
                    .filter(session -> session.isActive(Instant.now()))
                    .toList();
        }

        @Override
        public void revokeSession(String sessionId) {
            RefreshSession session = sessions.get(sessionId);
            if (session != null) {
                revokeFamily(session.familyId());
            }
        }

        @Override
        public void revokeFamily(String familyId) {
            Instant now = Instant.now();
            tokens.replaceAll((hash, token) -> familyId.equals(token.familyId()) ? token.revoke(now) : token);
            sessions.replaceAll((id, session) -> familyId.equals(session.familyId()) ? session.revoke(now) : session);
        }

        @Override
        public void revokeUser(Long accountId, String userType, Long userId) {
            findUserSessions(accountId, userType, userId).stream()
                    .map(RefreshSession::sessionId).forEach(this::revokeSession);
        }

        @Override
        public void revokeAccount(Long accountId) {
            sessions.values().stream().filter(session -> accountId.equals(session.accountId()))
                    .map(RefreshSession::sessionId).toList().forEach(this::revokeSession);
        }

        @Override
        public <T> T withFamilyLock(String familyId, Supplier<T> operation) {
            synchronized (locks.computeIfAbsent(familyId, ignored -> new Object())) {
                return operation.get();
            }
        }
    }
}
