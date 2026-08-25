package cn.iantech.gateway.model;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WebRequestModelCoverageTest {

    @Test
    void shouldConstructAllAuthenticationRequests() throws Exception {
        invokePrivateConstructor(AuthWebModels.class);

        assertEquals("admin", new AuthWebModels.AdminLoginRequest(
                "admin", "password", "web", "device").loginName());
        assertEquals("customer", new AuthWebModels.AppLoginRequest(
                "customer", "password", "app", "device").loginName());
        assertEquals("nickname", new AuthWebModels.RegisterRequest(
                "customer", "password", "nickname").displayName());
    }

    @Test
    void shouldConstructAllRbacRequests() throws Exception {
        invokePrivateConstructor(RbacWebRequests.class);

        assertEquals("nickname", new RbacWebRequests.UpdateUser(
                "password", "nickname", "user@example.com", "13800138000", true).displayName());
        assertEquals("ROLE_ADMIN", new RbacWebRequests.CreateRole(
                "ROLE_ADMIN", "管理员", "系统管理员", true).roleCode());
        assertEquals("ROLE_USER", new RbacWebRequests.UpdateRole(
                "ROLE_USER", "用户", "普通用户", true).roleCode());
        assertEquals("用户读取", new RbacWebRequests.UpdatePermission(
                "用户读取", 1, 0L, "/users", "GET", true).permName());
        assertEquals(List.of(1L), new RbacWebRequests.UserRoles(List.of(1L)).roleIds());
        assertEquals(List.of(2L), new RbacWebRequests.RolePermissions(List.of(2L)).permissionIds());
    }

    @Test
    void shouldConstructAllChannelRequests() throws Exception {
        invokePrivateConstructor(ChannelCredentialWebRequests.class);

        assertEquals("渠道A", new ChannelCredentialWebRequests.Update("渠道A").channelName());
        assertEquals(Boolean.TRUE, new ChannelCredentialWebRequests.UpdateStatus(true).status());
        assertEquals(List.of("1001"), new ChannelCredentialWebRequests.ReplaceScopes(
                List.of("1001")).scopeValues());
    }

    private void invokePrivateConstructor(Class<?> type) throws Exception {
        Constructor<?> constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);
        constructor.newInstance();
    }
}
