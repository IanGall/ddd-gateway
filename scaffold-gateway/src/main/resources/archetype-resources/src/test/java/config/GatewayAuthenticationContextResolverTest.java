package ${package}.config;

import cn.iantech.context.web.ResolvedAuthenticationContext;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayAuthenticationContextResolverTest {

    @Test
    void shouldResolveConfiguredTenantForAuthenticatedAdmin() {
        GatewayAuthenticationContextResolver resolver = new GatewayAuthenticationContextResolver(
                new GatewaySecurityProperties(1001L));

        ResolvedAuthenticationContext context = resolver.resolve("admin");

        assertEquals("admin", context.principalName());
        assertEquals("1001", context.tenantId());
        assertNull(context.userId());
    }

    @Test
    void shouldIgnoreMismatchedPrincipal() {
        GatewayAuthenticationContextResolver resolver = new GatewayAuthenticationContextResolver(
                new GatewaySecurityProperties(1001L));

        assertEquals("other", resolver.resolve("other").principalName());
    }

    @Test
    void shouldValidateTenantAsUnsignedIntegerRange() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();

            assertTrue(validator.validate(new GatewaySecurityProperties(1L)).isEmpty());
            assertTrue(validator.validate(new GatewaySecurityProperties(4_294_967_295L)).isEmpty());
            assertFalse(validator.validate(new GatewaySecurityProperties(0L)).isEmpty());
            assertFalse(validator.validate(new GatewaySecurityProperties(4_294_967_296L)).isEmpty());
        }
    }
}
