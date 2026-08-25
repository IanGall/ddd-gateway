package ${package}.controller;

import cn.iantech.api.model.auth.AuthIdentityDTO;
import cn.iantech.api.model.auth.AuthTokenDTO;
import cn.iantech.common.exception.AppException;
import ${package}.config.GatewayAuthFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;

class AuthControllerSupportCoverageTest {

    @Test
    void shouldRequireTrustedAccessTokenAttribute() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        assertThrows(AppException.class, () -> AuthControllerSupport.requiredAccessToken(request));

        request.setAttribute(GatewayAuthFilter.ACCESS_TOKEN_ATTRIBUTE, "   ");
        assertThrows(AppException.class, () -> AuthControllerSupport.requiredAccessToken(request));

        request.setAttribute(GatewayAuthFilter.ACCESS_TOKEN_ATTRIBUTE, "access-token");
        assertEquals("access-token", AuthControllerSupport.requiredAccessToken(request));
    }

    @Test
    void shouldNormalizeOptionalProtocolText() {
        assertNull(AuthControllerSupport.limited(null, 4));
        assertNull(AuthControllerSupport.limited("  ", 4));
        assertEquals("abc", AuthControllerSupport.limited("abc", 4));
        assertEquals("abcd", AuthControllerSupport.limited("abcdef", 4));
    }

    @Test
    void shouldRejectMissingAndWrongSubjectsAndAcceptExpectedSubjects() {
        assertThrows(AppException.class, () -> AuthControllerSupport.requireAdmin(null));
        assertThrows(AppException.class, () -> AuthControllerSupport.requireAdmin(AuthTokenDTO.builder().build()));
        assertThrows(AppException.class, () -> AuthControllerSupport.requireAdmin(token("CUSTOMER")));

        assertSame("ADMIN_PRIMARY", AuthControllerSupport.requireAdmin(token("ADMIN_PRIMARY"))
                .getIdentity().getSubjectType());
        assertSame("ADMIN_SUB_ACCOUNT", AuthControllerSupport.requireAdmin(token("ADMIN_SUB_ACCOUNT"))
                .getIdentity().getSubjectType());
        assertSame("CUSTOMER", AuthControllerSupport.requireApp(token("CUSTOMER"))
                .getIdentity().getSubjectType());
    }

    private AuthTokenDTO token(String subjectType) {
        return AuthTokenDTO.builder()
                .identity(AuthIdentityDTO.builder().subjectType(subjectType).build())
                .build();
    }
}
