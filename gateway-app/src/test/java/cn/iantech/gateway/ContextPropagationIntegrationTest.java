package cn.iantech.gateway;

import cn.iantech.api.model.rbac.RbacAccountDTO;
import cn.iantech.api.model.rbac.RbacAuthDTO;
import cn.iantech.context.core.ContextAccessor;
import cn.iantech.context.core.RequestContext;
import cn.iantech.gateway.service.GatewayRbacAuthenticator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "dubbo.registry.address=N/A",
                "dubbo.config-center.address=N/A",
                "dubbo.consumer.init=false",
                "gateway.security.platform.token=test-platform-token"
        })
@Import(ContextPropagationIntegrationTest.ContextController.class)
class ContextPropagationIntegrationTest {

    @MockitoBean
    private GatewayRbacAuthenticator authenticator;

    @org.junit.jupiter.api.BeforeEach
    void stubAuthentication() {
        org.mockito.Mockito.when(authenticator.authenticate(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> {
                    var request = (cn.iantech.api.model.rbac.AuthenticateRbacReq) invocation.getArgument(0);
                    return "test-password".equals(request.getPassword())
                            ? RbacAuthDTO.builder()
                            .userId(9_223_372_036_854_770_002L)
                            .accountId(9_223_372_036_854_770_001L)
                            .username("test-user")
                            .userType("SUB_ACCOUNT")
                            .roleCodes(java.util.List.of("OPERATOR"))
                            .permissionCodes(java.util.List.of("rbac:user:read"))
                            .build()
                            : null;
                });
        org.mockito.Mockito.when(authenticator.createAccount(org.mockito.ArgumentMatchers.any()))
                .thenReturn(RbacAccountDTO.builder().accountId(1001L).username("root")
                        .loginName("root@1001.com").build());
    }

    @LocalServerPort
    private int port;

    // 验证 Sa-Token 登录建立可信上下文，并忽略外部伪造的身份请求头
    @Test
    void shouldBuildTrustedContextFromTokenAndIgnoreForgedIdentityHeaders() throws Exception {
        HttpRequest loginRequest = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"loginName\":\"test-user@9223372036854770001.com\",\"password\":\"test-password\"}"))
                .build();
        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> loginResponse = client.send(loginRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, loginResponse.statusCode());
        String token = Pattern.compile("\\\"token\\\":\\\"([^\\\"]+)\\\"")
                .matcher(loginResponse.body()).results().findFirst().orElseThrow().group(1);
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/rbac/context"))
                .header("Authorization", "Bearer " + token)
                .header("X-Request-Id", "request-001")
                .header("X-User-Id", "forged-user")
                .header("X-Tenant-Id", "forged-tenant")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertEquals("request-001", response.headers().firstValue("X-Request-Id").orElseThrow());
        assertTrue(response.body().contains("\"principalName\":\"test-user\""));
        assertTrue(response.body().contains("\"source\":\"gateway\""));
        assertTrue(response.body().contains("\"tenantId\":\"9223372036854770001\""));
        assertTrue(response.body().contains("\"userId\":\"9223372036854770002\""));
        assertFalse(ContextAccessor.current().isPresent());
    }

    // 验证平台开户只能使用服务端令牌，且客户端不能指定主账号 ID
    @Test
    void shouldCreateAccountWithPlatformToken() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/platform/accounts"))
                .header("Content-Type", "application/json")
                .header("X-Platform-Token", "test-platform-token")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"username\":\"root\",\"password\":\"test-password\",\"displayName\":\"根账号\"}"))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"accountId\":1001"));
        assertTrue(response.body().contains("\"loginName\":\"root@1001.com\""));
    }

    // 验证错误凭据不会签发 Token
    @Test
    void shouldRejectInvalidCredentials() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"loginName\":\"test-user@9223372036854770001.com\",\"password\":\"wrong-password\"}"))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(401, response.statusCode());
        assertTrue(response.body().contains("AUTH_REQUIRED"));
    }

    // 验证 RBAC 路由拒绝匿名请求
    @Test
    void shouldRejectAnonymousRbacRequest() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create("http://127.0.0.1:" + port + "/api/rbac/context"))
                .GET()
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(401, response.statusCode());
        assertTrue(response.body().contains("AUTH_REQUIRED"));
    }

    @RestController
    static class ContextController {

        @GetMapping("/api/rbac/context")
        RequestContext currentContext() {
            return ContextAccessor.current().orElseThrow();
        }
    }
}
