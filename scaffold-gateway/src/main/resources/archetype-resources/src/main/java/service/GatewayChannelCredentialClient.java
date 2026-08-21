package ${package}.service;

import cn.iantech.api.IChannelCredentialService;
import cn.iantech.api.model.channel.*;
import ${package}.exception.GatewayRpcExceptionTranslator;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

/** 网关到渠道凭证管理服务的 RPC 适配器。 */
@Component
public class GatewayChannelCredentialClient {

    @DubboReference(version = "1.0.0", protocol = "tri", timeout = 10000, retries = 0, check = false)
    private IChannelCredentialService channelCredentialService;

    public ChannelCredentialSecretDTO create(CreateChannelCredentialReq request) { return invokeCredential(() -> channelCredentialService.create(request)); }
    public ChannelCredentialPageDTO queryPage(QueryChannelCredentialPageReq request) { return invokeCredential(() -> channelCredentialService.queryPage(request)); }
    public ChannelCredentialDTO queryById(QueryChannelCredentialByIdReq request) { return invokeCredential(() -> channelCredentialService.queryById(request)); }
    public ChannelCredentialDTO update(UpdateChannelCredentialReq request) { return invokeCredential(() -> channelCredentialService.update(request)); }
    public ChannelCredentialDTO updateStatus(UpdateChannelCredentialStatusReq request) { return invokeCredential(() -> channelCredentialService.updateStatus(request)); }
    public ChannelCredentialSecretDTO rotateSecret(RotateChannelCredentialSecretReq request) { return invokeCredential(() -> channelCredentialService.rotateSecret(request)); }
    public Boolean delete(DeleteChannelCredentialReq request) { return invokeCredential(() -> channelCredentialService.delete(request)); }
    public java.util.List<ChannelDataScopeDTO> queryDataScopes(QueryChannelDataScopesReq request) { return invokeCredential(() -> channelCredentialService.queryDataScopes(request)); }
    public java.util.List<ChannelDataScopeDTO> replaceDataScopes(ReplaceChannelDataScopesReq request) { return invokeCredential(() -> channelCredentialService.replaceDataScopes(request)); }
    private <T> T invokeCredential(java.util.function.Supplier<T> invocation) {
        try { return invocation.get(); } catch (RuntimeException exception) { throw GatewayRpcExceptionTranslator.translate(exception, cn.iantech.common.constant.Constants.ResponseCode.RPC_ERROR); }
    }
}
