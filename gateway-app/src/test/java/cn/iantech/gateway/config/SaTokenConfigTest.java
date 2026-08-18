package cn.iantech.gateway.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class SaTokenConfigTest {

    @Test
    void shouldUseTokenSessionForRoles() {
        SaTokenConfig config = new SaTokenConfig();
        assertNotNull(config.stpInterface());
    }
}
