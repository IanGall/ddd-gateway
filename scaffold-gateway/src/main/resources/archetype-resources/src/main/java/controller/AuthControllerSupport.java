package ${package}.controller;

import cn.iantech.api.model.auth.AuthIdentityDTO;
import cn.iantech.api.model.auth.AuthRefreshReq;
import cn.iantech.api.model.auth.AuthTokenDTO;
import cn.iantech.common.constant.Constants;
import cn.iantech.common.exception.AppException;
import ${package}.config.GatewayAuthFilter;
import ${package}.model.AuthWebModels;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Set;

/** 认证 Controller 共享的协议转换与主体校验。 */
final class AuthControllerSupport {

    private static final Set<String> ADMIN_SUBJECT_TYPES = Set.of("ADMIN_PRIMARY", "ADMIN_SUB_ACCOUNT");

    private AuthControllerSupport() {
    }

    static AuthTokenDTO requireAdmin(AuthTokenDTO issued) {
        return requireSubject(issued, ADMIN_SUBJECT_TYPES, "管理端认证身份无效");
    }

    static AuthTokenDTO requireApp(AuthTokenDTO issued) {
        return requireSubject(issued, Set.of("CUSTOMER"), "App 端认证身份无效");
    }

    static AuthRefreshReq refreshRequest(AuthWebModels.RefreshRequest request, HttpServletRequest servletRequest,
                                         String expectedSubjectType) {
        return AuthRefreshReq.builder()
                .refreshToken(request.refreshToken())
                .expectedSubjectType(expectedSubjectType)
                .clientType(limited(request.clientType(), 32))
                .deviceId(limited(request.deviceId(), 128))
                .ipAddress(limited(servletRequest.getRemoteAddr(), 64))
                .userAgent(limited(servletRequest.getHeader("User-Agent"), 256))
                .build();
    }

    static AuthWebModels.TokenResponse toResponse(AuthTokenDTO issued) {
        AuthIdentityDTO identity = issued.getIdentity();
        return new AuthWebModels.TokenResponse(issued.getAccessToken(), issued.getRefreshToken(), issued.getTokenType(),
                issued.getExpiresIn(), issued.getRefreshExpiresIn(), issued.getSessionId(), identity.getUserId(),
                identity.getAccountId(), identity.getUsername(), identity.getUserType());
    }

    static String requiredAccessToken(HttpServletRequest request) {
        String token = GatewayAuthFilter.accessToken(request);
        if (token == null || token.isBlank()) {
            throw new AppException(Constants.ResponseCode.AUTH_REQUIRED.getCode(), "需要认证");
        }
        return token;
    }

    static String limited(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private static AuthTokenDTO requireSubject(AuthTokenDTO issued, Set<String> expectedSubjectTypes, String message) {
        if (issued == null || issued.getIdentity() == null
                || !expectedSubjectTypes.contains(issued.getIdentity().getSubjectType())) {
            throw new AppException(Constants.ResponseCode.AUTH_REQUIRED.getCode(), message);
        }
        return issued;
    }
}
