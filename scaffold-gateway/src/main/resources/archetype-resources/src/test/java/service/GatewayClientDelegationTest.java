package ${package}.service;

import cn.iantech.api.IAuthService;
import cn.iantech.api.IChannelCredentialService;
import cn.iantech.api.ICustomerService;
import cn.iantech.api.IPlatformAccountService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;

class GatewayClientDelegationTest {

    @Test
    void shouldDelegateEveryAuthenticationOperation() {
        GatewayAuthClient client = new GatewayAuthClient();
        ReflectionTestUtils.setField(client, "authService", mock(IAuthService.class));
        ReflectionTestUtils.setField(client, "customerService", mock(ICustomerService.class));

        assertDoesNotThrow(() -> client.register("customer", "password", "昵称"));
        assertDoesNotThrow(() -> client.login(null));
        assertDoesNotThrow(() -> client.customerLogin(null));
        assertDoesNotThrow(() -> client.authenticateChannel(null));
        assertDoesNotThrow(() -> client.refresh(null));
        assertDoesNotThrow(() -> client.validate("access-token"));
        assertDoesNotThrow(() -> client.logout("access-token"));
        assertDoesNotThrow(() -> client.logoutAll("access-token"));
        assertDoesNotThrow(() -> client.sessions("access-token"));
        assertDoesNotThrow(() -> client.revokeSession("access-token", "session-id"));
    }

    @Test
    void shouldDelegateEveryChannelAndPlatformOperation() {
        GatewayChannelCredentialClient channelClient = new GatewayChannelCredentialClient();
        ReflectionTestUtils.setField(channelClient, "channelCredentialService", mock(IChannelCredentialService.class));
        GatewayRbacClient rbacClient = new GatewayRbacClient();
        ReflectionTestUtils.setField(rbacClient, "platformAccountService", mock(IPlatformAccountService.class));

        assertDoesNotThrow(() -> channelClient.create(null));
        assertDoesNotThrow(() -> channelClient.queryPage(null));
        assertDoesNotThrow(() -> channelClient.queryById(null));
        assertDoesNotThrow(() -> channelClient.update(null));
        assertDoesNotThrow(() -> channelClient.updateStatus(null));
        assertDoesNotThrow(() -> channelClient.rotateSecret(null));
        assertDoesNotThrow(() -> channelClient.delete(null));
        assertDoesNotThrow(() -> channelClient.queryDataScopes(null));
        assertDoesNotThrow(() -> channelClient.replaceDataScopes(null));
        assertDoesNotThrow(() -> rbacClient.createAccount(null));
    }
}
