package ${package}.model;

import java.util.List;

/** 登录和刷新成功后返回的双令牌及可信身份。 */
public record AuthTokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        long refreshExpiresIn,
        String sessionId,
        Long userId,
        Long accountId,
        String username,
        String userType,
        List<String> roleCodes,
        List<String> permissionCodes) {
}
