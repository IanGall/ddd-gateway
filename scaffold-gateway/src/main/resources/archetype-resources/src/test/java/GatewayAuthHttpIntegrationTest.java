package ${package};

import cn.iantech.api.model.auth.AuthIdentityDTO;
import cn.iantech.context.core.ContextAccessor;
import cn.iantech.context.core.RequestContext;
import ${package}.config.GatewayAuthFilter;
import ${package}.service.GatewayAuthClient;
import org.junit.jupiter.api.BeforeEach;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "dubbo.registry.address=N/A",
        "dubbo.registry.username=test-user",
        "dubbo.registry.password=test-password",
        "dubbo.registry.use-as-metadata-center=false",
        "dubbo.config-center.address=N/A",
        "dubbo.application.metadata-type=local",
        "dubbo.application.metadata-service-protocol=injvm",
        "dubbo.consumer.init=false"
})
@Import(GatewayAuthHttpIntegrationTest.ContextController.class)
class GatewayAuthHttpIntegrationTest {
    @MockitoBean
    private GatewayAuthClient authClient;
    @LocalServerPort
    private int port;

    @BeforeEach
    void resetClient() {
        reset(authClient);
    }

    @Test
    void shouldReturnUnifiedUnauthorizedResponse() throws Exception {
        HttpResponse<String> response = request(null, "request-001");
        assertEquals(401, response.statusCode());
        assertEquals("request-001", response.headers().firstValue("X-Request-Id").orElseThrow());
        assertTrue(response.body().contains("\"code\":\"AUTH_REQUIRED\""));
    }

    @Test
    void shouldPropagateValidatedIdentity() throws Exception {
        when(authClient.validate("opaque-token")).thenReturn(AuthIdentityDTO.builder()
                .subjectType("ADMIN_SUB_ACCOUNT").subjectId("200").tokenKind("OPAQUE")
                .accountId(100L).userId(200L).username("operator").userType("SUB_ACCOUNT")
                .sessionId("session-1").build());
        HttpResponse<String> response = request("opaque-token", "request-002");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"principalName\":\"operator\""));
        assertTrue(response.body().contains("\"tenantId\":\"100\""));
        assertTrue(response.body().contains("\"source\":\"gateway\""));
    }

    private HttpResponse<String> request(String token, String requestId) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(
                        URI.create("http://127.0.0.1:" + port + "/api/admin/test/context"))
                .header(GatewayAuthFilter.REQUEST_ID_HEADER, requestId).GET();
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    @RestController
    static class ContextController {
        @GetMapping("/api/admin/test/context")
        RequestContext context() {
            return ContextAccessor.current().orElseThrow();
        }
    }
}
