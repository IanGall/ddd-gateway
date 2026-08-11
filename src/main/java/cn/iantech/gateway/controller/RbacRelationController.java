package cn.iantech.gateway.controller;

import cn.iantech.api.IRbacService;
import cn.iantech.api.model.rbac.QueryRolePermissionIdsReq;
import cn.iantech.api.model.rbac.QueryRolePermissionIdsResp;
import cn.iantech.api.model.rbac.QueryUserRoleIdsReq;
import cn.iantech.api.model.rbac.QueryUserRoleIdsResp;
import cn.iantech.api.model.rbac.ReplaceRolePermissionsReq;
import cn.iantech.api.model.rbac.ReplaceUserRolesReq;
import cn.iantech.common.model.Response;
import cn.iantech.gateway.model.RbacWebRequests;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iantech.gateway.model.GatewayResponses.success;

@Validated
@RestController
@RequestMapping("/api/rbac")
public class RbacRelationController {

    @DubboReference(version = "1.0.0", timeout = 10000, retries = 0, check = false)
    private IRbacService rbacService;

    @PutMapping("/users/{userId}/roles")
    public Response<Boolean> replaceUserRoles(
            @Positive(message = "用户ID必须大于0") @PathVariable("userId") Long userId,
            @Valid @RequestBody RbacWebRequests.UserRoles request) {
        ReplaceUserRolesReq rpcRequest = ReplaceUserRolesReq.builder()
                .userId(userId)
                .roleIds(request.roleIds())
                .build();
        return success(rbacService.replaceUserRoles(rpcRequest));
    }

    @GetMapping("/users/{userId}/roles")
    public Response<QueryUserRoleIdsResp> queryUserRoleIds(
            @Positive(message = "用户ID必须大于0") @PathVariable("userId") Long userId) {
        return success(rbacService.queryUserRoleIds(QueryUserRoleIdsReq.builder().userId(userId).build()));
    }

    @PutMapping("/roles/{roleId}/permissions")
    public Response<Boolean> replaceRolePermissions(
            @Positive(message = "角色ID必须大于0") @PathVariable("roleId") Long roleId,
            @Valid @RequestBody RbacWebRequests.RolePermissions request) {
        ReplaceRolePermissionsReq rpcRequest = ReplaceRolePermissionsReq.builder()
                .roleId(roleId)
                .permissionIds(request.permissionIds())
                .build();
        return success(rbacService.replaceRolePermissions(rpcRequest));
    }

    @GetMapping("/roles/{roleId}/permissions")
    public Response<QueryRolePermissionIdsResp> queryRolePermissionIds(
            @Positive(message = "角色ID必须大于0") @PathVariable("roleId") Long roleId) {
        return success(rbacService.queryRolePermissionIds(
                QueryRolePermissionIdsReq.builder().roleId(roleId).build()));
    }

}
