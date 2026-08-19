package cn.iantech.gateway.controller;

import cn.iantech.api.model.customer.CustomerUserDTO;
import cn.iantech.common.model.Response;
import cn.iantech.gateway.service.GatewayCustomerClient;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iantech.gateway.model.GatewayResponses.success;

/**
 * C 端用户注册入口；登录统一使用 /auth/mobile-login。
 */
@RestController
@RequestMapping("/customer")
public class GatewayCustomerController {
    private final GatewayCustomerClient customerClient;

    public GatewayCustomerController(GatewayCustomerClient customerClient) {
        this.customerClient = customerClient;
    }

    @PostMapping("/register")
    public Response<CustomerUserDTO> register(@Valid @RequestBody RegisterRequest request) {
        return success(customerClient.register(request.mobile(), request.password(), request.displayName()));
    }

    public record RegisterRequest(
            @NotBlank(message = "手机号不能为空") @Size(max = 32, message = "手机号长度不能超过32") String mobile,
            @NotBlank(message = "密码不能为空") @Size(min = 8, max = 72, message = "密码长度必须为8到72位") String password,
            @Size(max = 128, message = "昵称长度不能超过128") String displayName) {
    }
}
