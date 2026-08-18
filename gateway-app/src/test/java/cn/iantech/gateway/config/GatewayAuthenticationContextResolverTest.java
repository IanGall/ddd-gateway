package cn.iantech.gateway.config;

import cn.iantech.context.web.ResolvedAuthenticationContext;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GatewayAuthenticationContextResolverTest {

    @Test
    void shouldResolveConfiguredTenantForAuthenticatedAdmin() {
        GatewayAuthenticationContextResolver resolver = new GatewayAuthenticationContextResolver(
                new GatewaySecurityProperties(4_294_967_295L));

        ResolvedAuthenticationContext context = resolver.resolve("admin");

        assertEquals("admin", context.principalName());
        assertEquals("4294967295", context.tenantId());
        assertNull(context.userId());
    }

    @Test
    void shouldIgnoreUnauthenticatedOrMismatchedPrincipal() {
        GatewayAuthenticationContextResolver resolver = new GatewayAuthenticationContextResolver(
                new GatewaySecurityProperties(1001L));

        assertNull(resolver.resolve(null).principalName());
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
