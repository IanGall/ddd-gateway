package cn.iantech.gateway.controller;

import cn.iantech.api.model.auth.AuthIdentityDTO;
import cn.iantech.api.model.auth.AuthTokenDTO;
import cn.iantech.gateway.config.GatewayAuthFilter;
import cn.iantech.gateway.model.AuthWebModels;
import cn.iantech.gateway.model.ChannelCredentialWebRequests;
import cn.iantech.gateway.model.RbacWebRequests;
import cn.iantech.gateway.service.GatewayAuthClient;
import cn.iantech.gateway.service.GatewayChannelCredentialClient;
import cn.iantech.gateway.service.GatewayRbacClient;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GatewayControllerDelegationTest {

    @Test
    void shouldDelegateAllRbacUserRoleAndPermissionOperations() {
        GatewayRbacClient client = mock(GatewayRbacClient.class);
        RbacUserController userController = new RbacUserController(client);
        RbacRoleController roleController = new RbacRoleController(client);
        RbacPermissionController permissionController = new RbacPermissionController(client);
        RbacRelationController relationController = new RbacRelationController(client);

        assertDoesNotThrow(() -> userController.createUser(new RbacWebRequests.CreateUser(
                "operator", "password", "操作员", "user@example.com", "13800138000", true)));
        assertDoesNotThrow(() -> userController.queryUserById(1L));
        assertDoesNotThrow(() -> userController.queryUserPage(1, 20, "operator", true));
        assertDoesNotThrow(() -> userController.updateUser(1L, new RbacWebRequests.UpdateUser(
                "new-password", "新名称", "new@example.com", "13900139000", true)));
        assertDoesNotThrow(() -> userController.deleteUser(1L));

        assertDoesNotThrow(() -> roleController.createRole(new RbacWebRequests.CreateRole(
                "ROLE_ADMIN", "管理员", "系统管理员", true)));
        assertDoesNotThrow(() -> roleController.queryRoleById(2L));
        assertDoesNotThrow(() -> roleController.queryRolePage(1, 20, "ROLE_ADMIN", "管理员", true));
        assertDoesNotThrow(() -> roleController.updateRole(2L, new RbacWebRequests.UpdateRole(
                "ROLE_ADMIN", "管理员", "系统管理员", true)));
        assertDoesNotThrow(() -> roleController.deleteRole(2L));

        assertDoesNotThrow(() -> permissionController.createPermission(new RbacWebRequests.CreatePermission(
                "user:read", "用户读取", 1, 0L, "/users", "GET", true)));
        assertDoesNotThrow(() -> permissionController.queryPermissionById(3L));
        assertDoesNotThrow(() -> permissionController.queryPermissionPage(
                1, 20, "user:read", "用户读取", 1, 0L, true));
        assertDoesNotThrow(() -> permissionController.updatePermission(3L,
                new RbacWebRequests.UpdatePermission("用户读取", 1, 0L, "/users", "GET", true)));
        assertDoesNotThrow(() -> permissionController.deletePermission(3L));

        assertDoesNotThrow(() -> relationController.replaceUserRoles(
                1L, new RbacWebRequests.UserRoles(List.of(2L))));
        assertDoesNotThrow(() -> relationController.queryUserRoleIds(1L));
        assertDoesNotThrow(() -> relationController.replaceRolePermissions(
                2L, new RbacWebRequests.RolePermissions(List.of(3L))));
        assertDoesNotThrow(() -> relationController.queryRolePermissionIds(2L));
    }

    @Test
    void shouldDelegateAllChannelCredentialOperations() {
        ChannelCredentialController controller = new ChannelCredentialController(
                mock(GatewayChannelCredentialClient.class));

        assertDoesNotThrow(() -> controller.create(new ChannelCredentialWebRequests.Create("渠道A")));
        assertDoesNotThrow(() -> controller.queryPage(1, 20, "CHANNEL_A", "渠道A", true));
        assertDoesNotThrow(() -> controller.queryById(1L));
        assertDoesNotThrow(() -> controller.update(1L, new ChannelCredentialWebRequests.Update("渠道B")));
        assertDoesNotThrow(() -> controller.updateStatus(
                1L, new ChannelCredentialWebRequests.UpdateStatus(true)));
        assertDoesNotThrow(() -> controller.rotateSecret(1L));
        assertDoesNotThrow(() -> controller.delete(1L));
        assertDoesNotThrow(() -> controller.queryDataScopes(1L, "STORE"));
        assertDoesNotThrow(() -> controller.replaceDataScopes(
                1L, "STORE", new ChannelCredentialWebRequests.ReplaceScopes(List.of("1001"))));
    }

    @Test
    void shouldDelegateAllAdminAndAppAuthenticationOperations() {
        GatewayAuthClient client = mock(GatewayAuthClient.class);
        when(client.login(any())).thenReturn(token("ADMIN_PRIMARY"));
        when(client.customerLogin(any())).thenReturn(token("CUSTOMER"));
        when(client.refresh(any())).thenAnswer(invocation -> {
            String subjectType = invocation.<cn.iantech.api.model.auth.AuthRefreshReq>getArgument(0)
                    .getExpectedSubjectType();
            return token("ADMIN".equals(subjectType) ? "ADMIN_PRIMARY" : subjectType);
        });
        AdminAuthController adminController = new AdminAuthController(client);
        AppAuthController appController = new AppAuthController(client);
        MockHttpServletRequest request = authenticatedRequest();

        assertDoesNotThrow(() -> adminController.login(
                new AuthWebModels.AdminLoginRequest("admin", "password", "web", "device"), request));
        assertDoesNotThrow(() -> adminController.refresh(
                new AuthWebModels.RefreshRequest("refresh-token", "web", "device"), request));
        assertDoesNotThrow(() -> adminController.logout(request));
        assertDoesNotThrow(() -> adminController.logoutAll(request));
        assertDoesNotThrow(() -> adminController.sessions(request));
        assertDoesNotThrow(() -> adminController.revokeSession("session-id", request));

        assertDoesNotThrow(() -> appController.register(
                new AuthWebModels.RegisterRequest("customer", "password", "昵称")));
        assertDoesNotThrow(() -> appController.login(
                new AuthWebModels.AppLoginRequest("customer", "password", "app", "device"), request));
        assertDoesNotThrow(() -> appController.refresh(
                new AuthWebModels.RefreshRequest("refresh-token", "app", "device"), request));
        assertDoesNotThrow(() -> appController.logout(request));
        assertDoesNotThrow(() -> appController.logoutAll(request));
        assertDoesNotThrow(() -> appController.sessions(request));
        assertDoesNotThrow(() -> appController.revokeSession("session-id", request));
    }

    private MockHttpServletRequest authenticatedRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(GatewayAuthFilter.ACCESS_TOKEN_ATTRIBUTE, "access-token");
        request.addHeader("User-Agent", "JUnit");
        request.setRemoteAddr("127.0.0.1");
        return request;
    }

    private AuthTokenDTO token(String subjectType) {
        return AuthTokenDTO.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .tokenType("Bearer")
                .expiresIn(300)
                .refreshExpiresIn(600)
                .sessionId("session-id")
                .identity(AuthIdentityDTO.builder()
                        .accountId(1L)
                        .userId(2L)
                        .username("operator")
                        .userType(subjectType)
                        .subjectType(subjectType)
                        .build())
                .build();
    }
}
