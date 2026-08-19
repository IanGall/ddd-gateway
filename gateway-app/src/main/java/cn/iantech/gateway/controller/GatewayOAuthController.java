package cn.iantech.gateway.controller;

import cn.iantech.api.model.oauth.*;
import cn.iantech.common.constant.Constants;
import cn.iantech.common.exception.AppException;
import cn.iantech.common.model.Response;
import cn.iantech.gateway.service.GatewayAuthClient;
import cn.iantech.gateway.service.GatewayOAuthClient;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import static cn.iantech.gateway.model.GatewayResponses.success;

@RestController
@RequestMapping("/oauth2")
public class GatewayOAuthController {
    private final GatewayOAuthClient oauthClient;
    private final GatewayAuthClient authClient;
    private final String internalToken;

    public GatewayOAuthController(GatewayOAuthClient oauthClient, GatewayAuthClient authClient,
                                  @Value("${oauth2.internal-token:${oauth2.client.secret:change-me-in-production}}") String internalToken) {
        this.oauthClient = oauthClient;
        this.authClient = authClient;
        this.internalToken = internalToken;
    }

    @GetMapping("/authorize")
    public Response<AuthorizationResponse> authorize(@Valid @ModelAttribute AuthorizeRequest request,
                                                     HttpServletRequest servletRequest) {
        String bearer = servletRequest.getHeader("Authorization");
        if (bearer == null || !bearer.startsWith("Bearer "))
            throw new AppException(Constants.ResponseCode.AUTH_REQUIRED.getCode(), "需要用户登录后授权");
        var identity = authClient.validate(bearer.substring(7).trim());
        OAuthAuthorizeReq req = new OAuthAuthorizeReq();
        req.setClientId(request.clientId());
        req.setRedirectUri(request.redirectUri());
        req.setResponseType(request.responseType());
        req.setScope(request.scope());
        req.setState(request.state());
        req.setCodeChallenge(request.codeChallenge());
        req.setCodeChallengeMethod(request.codeChallengeMethod());
        req.setSubjectType(identity.getSubjectType());
        req.setSubjectId(identity.getSubjectId());
        return success(new AuthorizationResponse(oauthClient.authorize(req), request.state()));
    }

    @PostMapping("/token")
    public Response<OAuthTokenDTO> token(@Valid @RequestBody OAuthTokenReq request) {
        return success(oauthClient.token(request));
    }

    @PostMapping("/revoke")
    public Response<Void> revoke(@Valid @RequestBody RevokeRequest request, HttpServletRequest servletRequest) {
        OAuthIntrospectReq revoke = new OAuthIntrospectReq();
        revoke.setToken(request.token());
        revoke.setClientId(request.clientId());
        revoke.setClientSecret(request.clientSecret());
        oauthClient.revoke(revoke);
        return success(null);
    }

    @PostMapping("/introspect")
    public Response<OAuthIntrospectDTO> introspect(@Valid @RequestBody IntrospectRequest request, HttpServletRequest servletRequest) {
        requireInternal(servletRequest);
        return success(oauthClient.introspect(request.token()));
    }

    private void requireInternal(HttpServletRequest request) {
        if (!internalToken.equals(request.getHeader("X-OAuth-Internal-Token")))
            throw new AppException(Constants.ResponseCode.ACCESS_DENIED.getCode(), "仅允许内部资源服务调用");
    }

    public record AuthorizeRequest(@NotBlank String clientId, @NotBlank String redirectUri,
                                   @NotBlank String responseType,
                                   @NotBlank String scope, String state, @NotBlank String codeChallenge,
                                   @NotBlank String codeChallengeMethod) {
    }

    public record AuthorizationResponse(String code, String state) {
    }

    public record RevokeRequest(@NotBlank String token, @NotBlank String clientId, @NotBlank String clientSecret) {
    }

    public record IntrospectRequest(@NotBlank String token) {
    }
}
