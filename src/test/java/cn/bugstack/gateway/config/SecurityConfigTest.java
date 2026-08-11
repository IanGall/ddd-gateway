package cn.bugstack.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityConfigTest {

    @Test
    void 应创建仅具备Rbac管理员角色的账号() {
        SecurityConfig config = new SecurityConfig();
        PasswordEncoder encoder = config.passwordEncoder();
        GatewaySecurityProperties properties = new GatewaySecurityProperties("admin", "secure-password");
        UserDetailsService service = config.userDetailsService(properties, encoder);

        UserDetails user = service.loadUserByUsername("admin");

        assertTrue(encoder.matches("secure-password", user.getPassword()));
        assertTrue(user.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_RBAC_ADMIN".equals(authority.getAuthority())));
    }
}
