package cn.iantech.gateway.service;

import cn.iantech.api.*;
import cn.iantech.api.model.channel.ChannelSignatureVerifyReq;
import cn.iantech.api.model.customer.CustomerLoginReq;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;

class GatewayClientDelegationTest {

    @Test
    void shouldDelegateEveryRbacOperation() {
        GatewayRbacClient client = new GatewayRbacClient();
        ReflectionTestUtils.setField(client, "rbacService", mock(IRbacService.class));
        ReflectionTestUtils.setField(client, "platformAccountService", mock(IPlatformAccountService.class));

        assertDoesNotThrow(() -> client.createAccount(null));
        assertDoesNotThrow(() -> client.createUser(null));
        assertDoesNotThrow(() -> client.queryUserById(1L));
        assertDoesNotThrow(() -> client.queryUserPage(null));
        assertDoesNotThrow(() -> client.updateUser(null));
        assertDoesNotThrow(() -> client.deleteUser(null));
        assertDoesNotThrow(() -> client.createRole(null));
        assertDoesNotThrow(() -> client.queryRoleById(1L));
        assertDoesNotThrow(() -> client.queryRolePage(null));
        assertDoesNotThrow(() -> client.updateRole(null));
        assertDoesNotThrow(() -> client.deleteRole(null));
        assertDoesNotThrow(() -> client.createPermission(null));
        assertDoesNotThrow(() -> client.queryPermissionById(1L));
        assertDoesNotThrow(() -> client.queryPermissionPage(null));
        assertDoesNotThrow(() -> client.updatePermission(null));
        assertDoesNotThrow(() -> client.deletePermission(null));
        assertDoesNotThrow(() -> client.replaceUserRoles(null));
        assertDoesNotThrow(() -> client.replaceRolePermissions(null));
        assertDoesNotThrow(() -> client.queryUserRoleIds(null));
        assertDoesNotThrow(() -> client.queryRolePermissionIds(null));
    }

    @Test
    void shouldDelegateEveryChannelCredentialOperation() {
        GatewayChannelCredentialClient client = new GatewayChannelCredentialClient();
        ReflectionTestUtils.setField(client, "channelCredentialService", mock(IChannelCredentialService.class));

        assertDoesNotThrow(() -> client.create(null));
        assertDoesNotThrow(() -> client.queryPage(null));
        assertDoesNotThrow(() -> client.queryById(null));
        assertDoesNotThrow(() -> client.update(null));
        assertDoesNotThrow(() -> client.updateStatus(null));
        assertDoesNotThrow(() -> client.rotateSecret(null));
        assertDoesNotThrow(() -> client.delete(null));
        assertDoesNotThrow(() -> client.queryDataScopes(null));
        assertDoesNotThrow(() -> client.replaceDataScopes(null));
    }

    @Test
    void shouldDelegateEveryAuthenticationOperation() {
        GatewayAuthClient client = new GatewayAuthClient();
        ReflectionTestUtils.setField(client, "authService", mock(IAuthService.class));
        ReflectionTestUtils.setField(client, "customerService", mock(ICustomerService.class));

        assertDoesNotThrow(() -> client.register("customer", "password", "昵称"));
        assertDoesNotThrow(() -> client.login(null));
        assertDoesNotThrow(() -> client.customerLogin(new CustomerLoginReq()));
        assertDoesNotThrow(() -> client.authenticateChannel(new ChannelSignatureVerifyReq()));
        assertDoesNotThrow(() -> client.refresh(null));
        assertDoesNotThrow(() -> client.validate("access-token"));
        assertDoesNotThrow(() -> client.logout("access-token"));
        assertDoesNotThrow(() -> client.logoutAll("access-token"));
        assertDoesNotThrow(() -> client.sessions("access-token"));
        assertDoesNotThrow(() -> client.revokeSession("access-token", "session-id"));
    }
}
