package ${package}.controller;

import cn.iantech.api.model.rbac.CreateRbacAccountReq;
import cn.iantech.api.model.rbac.RbacAccountDTO;
import cn.iantech.common.constant.Constants;
import cn.iantech.common.exception.AppException;
import cn.iantech.common.model.Response;
import ${package}.config.PlatformSecurityProperties;
import ${package}.service.GatewayRbacAuthenticator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import static ${package}.model.GatewayResponses.success;

@RestController
@RequestMapping("/platform/accounts")
public class PlatformAccountController {
    private final PlatformSecurityProperties properties;
    private final GatewayRbacAuthenticator authenticator;

    public PlatformAccountController(PlatformSecurityProperties properties, GatewayRbacAuthenticator authenticator) {
        this.properties = properties;
        this.authenticator = authenticator;
    }

    @PostMapping
    public Response<RbacAccountDTO> createAccount(@RequestHeader("X-Platform-Token") String platformToken,
                                                   @Valid @RequestBody CreateAccountRequest request) {
        if (!constantTimeEquals(properties.token(), platformToken)) {
            throw new AppException(Constants.ResponseCode.ACCESS_DENIED.getCode(), "平台凭据无效");
        }
        return success(authenticator.createAccount(CreateRbacAccountReq.builder()
                .username(request.username()).password(request.password()).displayName(request.displayName())
                .email(request.email()).mobile(request.mobile()).build()));
    }

    private boolean constantTimeEquals(String expected, String actual) {
        return actual != null && MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }

    public record CreateAccountRequest(@NotBlank @Size(max = 64) String username,
                                       @NotBlank @Size(min = 8, max = 72) String password,
                                       @Size(max = 128) String displayName,
                                       @Email @Size(max = 128) String email,
                                       @Size(max = 32) String mobile) { }
}
