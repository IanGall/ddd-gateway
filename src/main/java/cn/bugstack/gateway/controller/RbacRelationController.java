package cn.bugstack.gateway.controller;

import cn.bugstack.api.IRbacRelationService;
import cn.bugstack.api.model.rbac.QueryRolePermissionIdsReq;
import cn.bugstack.api.model.rbac.QueryRolePermissionIdsResp;
import cn.bugstack.api.model.rbac.QueryUserRoleIdsReq;
import cn.bugstack.api.model.rbac.QueryUserRoleIdsResp;
import cn.bugstack.api.model.rbac.ReplaceRolePermissionsReq;
import cn.bugstack.api.model.rbac.ReplaceUserRolesReq;
import cn.bugstack.common.constant.Constants;
import cn.bugstack.common.exception.AppException;
import cn.bugstack.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.function.Supplier;

@Slf4j
@RestController
@RequestMapping("/api/rbac")
public class RbacRelationController {

    @DubboReference(version = "1.0.0", timeout = 10000, retries = 0, check = false)
    private IRbacRelationService rbacRelationService;

    @PutMapping("/users/{userId}/roles")
    public Response<Boolean> replaceUserRoles(@PathVariable("userId") Long userId,
                                              @RequestBody(required = false) ReplaceUserRolesReq req) {
        return execute(() -> {
            ReplaceUserRolesReq replaceReq = Objects.isNull(req) ? new ReplaceUserRolesReq() : req;
            replaceReq.setUserId(userId);
            return rbacRelationService.replaceUserRoles(replaceReq);
        });
    }

    @GetMapping("/users/{userId}/roles")
    public Response<QueryUserRoleIdsResp> queryUserRoleIds(@PathVariable("userId") Long userId) {
        return execute(() -> rbacRelationService.queryUserRoleIds(QueryUserRoleIdsReq.builder().userId(userId).build()));
    }

    @PutMapping("/roles/{roleId}/permissions")
    public Response<Boolean> replaceRolePermissions(@PathVariable("roleId") Long roleId,
                                                    @RequestBody(required = false) ReplaceRolePermissionsReq req) {
        return execute(() -> {
            ReplaceRolePermissionsReq replaceReq = Objects.isNull(req) ? new ReplaceRolePermissionsReq() : req;
            replaceReq.setRoleId(roleId);
            return rbacRelationService.replaceRolePermissions(replaceReq);
        });
    }

    @GetMapping("/roles/{roleId}/permissions")
    public Response<QueryRolePermissionIdsResp> queryRolePermissionIds(@PathVariable("roleId") Long roleId) {
        return execute(() -> rbacRelationService.queryRolePermissionIds(QueryRolePermissionIdsReq.builder().roleId(roleId).build()));
    }

    private <T> Response<T> execute(Supplier<T> supplier) {
        try {
            return success(supplier.get());
        } catch (AppException appException) {
            log.warn("RBAC 关系接口业务异常：code={}, info={}", appException.getCode(), appException.getInfo());
            return Response.<T>builder()
                    .code(appException.getCode())
                    .info(Objects.isNull(appException.getInfo()) ? Constants.ResponseCode.UN_ERROR.getInfo() : appException.getInfo())
                    .build();
        } catch (Exception exception) {
            log.error("RBAC 关系接口系统异常", exception);
            return Response.<T>builder()
                    .code(Constants.ResponseCode.UN_ERROR.getCode())
                    .info(Constants.ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    private <T> Response<T> success(T data) {
        return Response.<T>builder()
                .code(Constants.ResponseCode.SUCCESS.getCode())
                .info(Constants.ResponseCode.SUCCESS.getInfo())
                .data(data)
                .build();
    }

}
