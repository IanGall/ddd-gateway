package ${package}.service;

import cn.iantech.api.IRbacService;
import cn.iantech.api.model.rbac.AuthenticateRbacAdminReq;
import cn.iantech.api.model.rbac.RbacAdminAuthDTO;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

/** 网关到标准工程的管理员认证 RPC 适配器。 */
@Component
public class GatewayRbacAuthenticator {

    @DubboReference(version = "1.0.0", protocol = "tri", timeout = 10000, retries = 0, check = false)
    private IRbacService rbacService;

    public RbacAdminAuthDTO authenticate(AuthenticateRbacAdminReq request) {
        return rbacService.authenticateAdmin(request);
    }
}
