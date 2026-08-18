package cn.iantech.gateway.service;

import cn.iantech.api.IRbacService;
import cn.iantech.api.model.rbac.CreateRbacAccountReq;
import cn.iantech.api.model.rbac.RbacAccountDTO;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

/**
 * 网关到标准工程的平台开户 RPC 适配器。
 */
@Component
public class GatewayRbacAuthenticator {

    @DubboReference(version = "1.0.0", protocol = "tri", timeout = 10000, retries = 0, check = false)
    private IRbacService rbacService;

    public RbacAccountDTO createAccount(CreateRbacAccountReq request) {
        return rbacService.createAccount(request);
    }
}
