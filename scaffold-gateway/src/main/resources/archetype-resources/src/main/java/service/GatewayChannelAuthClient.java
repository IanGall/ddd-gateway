package ${package}.service;

import cn.iantech.api.IChannelAuthService;
import cn.iantech.api.model.auth.AuthIdentityDTO;
import cn.iantech.api.model.channel.ChannelSignatureVerifyReq;
import ${package}.exception.GatewayRpcExceptionTranslator;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

import static cn.iantech.common.constant.Constants.ResponseCode.AUTH_UNAVAILABLE;

/** 网关到渠道认证服务的单次 RPC 适配器。 */
@Component
public class GatewayChannelAuthClient {

    @DubboReference(version = "1.0.0", protocol = "tri", timeout = 10000, retries = 0, check = false)
    private IChannelAuthService channelAuthService;

    public AuthIdentityDTO authenticate(ChannelSignatureVerifyReq request) {
        try {
            return channelAuthService.authenticate(request);
        } catch (RuntimeException exception) {
            throw GatewayRpcExceptionTranslator.translate(exception, AUTH_UNAVAILABLE);
        }
    }
}
