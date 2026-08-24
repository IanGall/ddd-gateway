package ${package}.controller;

import cn.iantech.api.model.auth.AuthIdentityDTO;
import cn.iantech.api.model.auth.AuthRefreshReq;
import cn.iantech.api.model.auth.AuthSubjectTypes;
import cn.iantech.api.model.auth.AuthTokenDTO;
import cn.iantech.common.exception.AppException;
import ${package}.model.AuthWebModels;
import ${package}.service.GatewayAuthClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdminAuthControllerTest {
    @Test
    void shouldFixAdminSubjectWhenRefreshing() {
        GatewayAuthClient client = mock(GatewayAuthClient.class);
        when(client.refresh(any())).thenReturn(token(AuthSubjectTypes.ADMIN_PRIMARY));
        new AdminAuthController(client).refresh(new AuthWebModels.RefreshRequest("refresh", "web", "device"),
                new MockHttpServletRequest());
        ArgumentCaptor<AuthRefreshReq> captor = ArgumentCaptor.forClass(AuthRefreshReq.class);
        verify(client).refresh(captor.capture());
        assertEquals(AuthSubjectTypes.ADMIN, captor.getValue().getExpectedSubjectType());
    }

    @Test
    void shouldRejectCustomerIdentityReturnedToAdminEndpoint() {
        GatewayAuthClient client = mock(GatewayAuthClient.class);
        when(client.refresh(any())).thenReturn(token(AuthSubjectTypes.CUSTOMER));
        assertThrows(AppException.class, () -> new AdminAuthController(client).refresh(
                new AuthWebModels.RefreshRequest("refresh", "web", "device"), new MockHttpServletRequest()));
    }

    private AuthTokenDTO token(String subjectType) {
        return AuthTokenDTO.builder().accessToken("access").refreshToken("refresh").tokenType("Bearer")
                .sessionId("session").identity(AuthIdentityDTO.builder().subjectType(subjectType).userId(1L).build()).build();
    }
}
