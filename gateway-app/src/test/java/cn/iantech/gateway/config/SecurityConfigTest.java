package cn.iantech.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityConfigTest {

    // 验证管理员账号只具备 RBAC 管理员角色
    @Test
    void shouldCreateAccountWithOnlyRbacAdminRole() {
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
