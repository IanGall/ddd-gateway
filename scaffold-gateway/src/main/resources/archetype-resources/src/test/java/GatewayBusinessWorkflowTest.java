package ${package};

import cn.iantech.api.model.auth.AuthIdentityDTO;
import cn.iantech.api.model.auth.AuthLoginReq;
import cn.iantech.api.model.auth.AuthTokenDTO;
import ${package}.service.GatewayAuthClient;
import cn.iantech.test.flow.BusinessFlow;
import cn.iantech.test.flow.BusinessFlowRunner;
import cn.iantech.test.flow.FeignTestClientFactory;
import cn.iantech.test.flow.FlowKey;
import cn.iantech.test.nplusone.DetectNPlusOne;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.Serial;
import java.io.Serializable;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 在网关入口验证登录、Token 传递和受保护接口访问的完整流程。 */
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
class GatewayBusinessWorkflowTest {
    private static final String ACCESS_TOKEN = "opaque-workflow-token";

    @MockitoBean
    private GatewayAuthClient authClient;

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        reset(authClient);
        AuthIdentityDTO identity = AuthIdentityDTO.builder()
                .accountId(1001L).userId(2001L).username("operator").userType("SUB_ACCOUNT")
                .subjectType("ADMIN_SUB_ACCOUNT").subjectId("2001").tokenKind("OPAQUE")
                .sessionId("session-workflow").build();
        when(authClient.login(any(AuthLoginReq.class))).thenReturn(AuthTokenDTO.builder()
                .accessToken(ACCESS_TOKEN).refreshToken("opaque-refresh-token").tokenType("Bearer")
                .expiresIn(900).refreshExpiresIn(2_592_000).sessionId(identity.getSessionId()).identity(identity).build());
        when(authClient.validate(ACCESS_TOKEN)).thenReturn(identity);
    }

    @Test
    @DetectNPlusOne(maxRemoteCalls = 2, maxRepeatedRemoteCalls = 1)
    void shouldLoginAndAccessProtectedGatewayApi() {
        GatewayWorkflowApi api = FeignTestClientFactory.create(GatewayWorkflowApi.class,
                URI.create("http://127.0.0.1:" + port));
        FlowKey<TokenResponse> tokenKey = new FlowKey<>("token", TokenResponse.class);
        FlowKey<StatusResponse> statusKey = new FlowKey<>("status", StatusResponse.class);
        BusinessFlow flow = BusinessFlow.builder("网关登录与状态查询")
                .then("登录", context -> context.put(tokenKey,
                        api.login(new LoginRequest("operator@1001.com", "test-password", "test", "device-1")).data()))
                .then("访问受保护接口", context -> context.put(statusKey,
                        api.status(context.require(tokenKey).accessToken()).data()))
                .then("验证状态", context -> assertEquals("UP", context.require(statusKey).status()))
                .build();

        assertEquals("UP", new BusinessFlowRunner().run(flow).require(statusKey).status());
        verify(authClient).login(any(AuthLoginReq.class));
        verify(authClient).validate(ACCESS_TOKEN);
    }

    interface GatewayWorkflowApi {
        @RequestLine("POST /api/admin/auth/login")
        @Headers("Content-Type: application/json")
        ApiResponse<TokenResponse> login(LoginRequest request);

        @RequestLine("GET /api/admin/status")
        @Headers("Authorization: Bearer {token}")
        ApiResponse<StatusResponse> status(@Param("token") String token);
    }

    record LoginRequest(String loginName, String password, String clientType, String deviceId) implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
    }

    record ApiResponse<T>(String code, String info, T data) implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
    }

    record TokenResponse(String accessToken) implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
    }

    record StatusResponse(String application, String status) implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
    }
}
