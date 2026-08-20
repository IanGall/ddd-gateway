package cn.iantech.gateway.service;

import cn.iantech.api.ICustomerService;
import cn.iantech.api.model.customer.CustomerUserDTO;
import cn.iantech.gateway.exception.GatewayRpcExceptionTranslator;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

import static cn.iantech.common.constant.Constants.ResponseCode.AUTH_UNAVAILABLE;

/**
 * 网关到 C 端用户服务的 RPC 适配器。
 */
@Component
public class GatewayCustomerClient {
    @DubboReference(version = "1.0.0", protocol = "tri", timeout = 10000, retries = 0, check = false)
    private ICustomerService customerService;

    public CustomerUserDTO register(String loginName, String password, String displayName) {
        try {
            return customerService.register(loginName, password, displayName);
        } catch (RuntimeException exception) {
            throw GatewayRpcExceptionTranslator.translate(exception, AUTH_UNAVAILABLE);
        }
    }
}
