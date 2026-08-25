package ${package}.model;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WebRequestModelCoverageTest {

    @Test
    void shouldConstructEveryWebRequestModel() throws Exception {
        invokePrivateConstructor(AuthWebModels.class);
        invokePrivateConstructor(ChannelCredentialWebRequests.class);

        assertEquals("admin", new AuthWebModels.AdminLoginRequest(
                "admin", "password", "web", "device").loginName());
        assertEquals("customer", new AuthWebModels.AppLoginRequest(
                "customer", "password", "app", "device").loginName());
        assertEquals("昵称", new AuthWebModels.AppRegisterRequest(
                "customer", "password", "昵称").displayName());
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
