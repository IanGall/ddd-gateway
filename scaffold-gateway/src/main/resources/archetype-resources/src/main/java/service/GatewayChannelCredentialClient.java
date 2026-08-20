package ${package}.service;

import cn.iantech.api.IChannelCredentialService;
import cn.iantech.api.model.channel.*;
import ${package}.exception.GatewayRpcExceptionTranslator;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;
import java.util.List;

import java.util.function.Supplier;

import static cn.iantech.common.constant.Constants.ResponseCode.RPC_ERROR;

/** 网关到渠道凭证管理服务的 RPC 适配器。 */
@Component
public class GatewayChannelCredentialClient {

    @DubboReference(version = "1.0.0", protocol = "tri", timeout = 10000, retries = 0, check = false)
    private IChannelCredentialService channelCredentialService;

    public ChannelCredentialSecretDTO create(CreateChannelCredentialReq request) {
        return invoke(() -> channelCredentialService.create(request));
    }

    public ChannelCredentialPageDTO queryPage(QueryChannelCredentialPageReq request) {
        return invoke(() -> channelCredentialService.queryPage(request));
    }

    public ChannelCredentialDTO queryById(QueryChannelCredentialByIdReq request) {
        return invoke(() -> channelCredentialService.queryById(request));
    }

    public ChannelCredentialDTO update(UpdateChannelCredentialReq request) {
        return invoke(() -> channelCredentialService.update(request));
    }

    public ChannelCredentialDTO updateStatus(UpdateChannelCredentialStatusReq request) {
        return invoke(() -> channelCredentialService.updateStatus(request));
    }

    public ChannelCredentialSecretDTO rotateSecret(RotateChannelCredentialSecretReq request) {
        return invoke(() -> channelCredentialService.rotateSecret(request));
    }

    public Boolean delete(DeleteChannelCredentialReq request) {
        return invoke(() -> channelCredentialService.delete(request));
    }

    private <T> T invoke(Supplier<T> invocation) {
        try {
            return invocation.get();
        } catch (RuntimeException exception) {
            throw GatewayRpcExceptionTranslator.translate(exception, RPC_ERROR);
        }
    }

    public List<ChannelDataScopeDTO> queryDataScopes(QueryChannelDataScopesReq request) {
        return invoke(() -> channelCredentialService.queryDataScopes(request));
    }

    public List<ChannelDataScopeDTO> replaceDataScopes(ReplaceChannelDataScopesReq request) {
        return invoke(() -> channelCredentialService.replaceDataScopes(request));
    }
}
