package cn.bugstack.gateway.controller;

import cn.bugstack.api.IRbacService;
import cn.bugstack.api.model.rbac.CreateRbacPermissionReq;
import cn.bugstack.api.model.rbac.CreateRbacRoleReq;
import cn.bugstack.api.model.rbac.CreateRbacUserReq;
import cn.bugstack.api.model.rbac.DeleteRbacPermissionReq;
import cn.bugstack.api.model.rbac.DeleteRbacRoleReq;
import cn.bugstack.api.model.rbac.DeleteRbacUserReq;
import cn.bugstack.api.model.rbac.QueryRbacPermissionPageReq;
import cn.bugstack.api.model.rbac.QueryRbacRolePageReq;
import cn.bugstack.api.model.rbac.QueryRbacUserPageReq;
import cn.bugstack.api.model.rbac.QueryRolePermissionIdsReq;
import cn.bugstack.api.model.rbac.QueryRolePermissionIdsResp;
import cn.bugstack.api.model.rbac.QueryUserRoleIdsReq;
import cn.bugstack.api.model.rbac.QueryUserRoleIdsResp;
import cn.bugstack.api.model.rbac.RbacPermissionDTO;
import cn.bugstack.api.model.rbac.RbacPermissionPageDTO;
import cn.bugstack.api.model.rbac.RbacRoleDTO;
import cn.bugstack.api.model.rbac.RbacRolePageDTO;
import cn.bugstack.api.model.rbac.RbacUserDTO;
import cn.bugstack.api.model.rbac.RbacUserPageDTO;
import cn.bugstack.api.model.rbac.ReplaceRolePermissionsReq;
import cn.bugstack.api.model.rbac.ReplaceUserRolesReq;
import cn.bugstack.api.model.rbac.UpdateRbacPermissionReq;
import cn.bugstack.api.model.rbac.UpdateRbacRoleReq;
import cn.bugstack.api.model.rbac.UpdateRbacUserReq;
import cn.bugstack.common.constant.Constants;
import cn.bugstack.common.exception.AppException;
import cn.bugstack.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.function.Supplier;

@Slf4j
@RestController
@RequestMapping("/api/rbac")
public class RbacController {

    @DubboReference(version = "1.0.0", timeout = 10000, retries = 0, check = false)
    private IRbacService rbacService;

    @PostMapping("/users")
    public Response<RbacUserDTO> createUser(@RequestBody CreateRbacUserReq req) {
        return execute(() -> rbacService.createUser(req));
    }

    @GetMapping("/users/{id}")
    public Response<RbacUserDTO> queryUserById(@PathVariable("id") Long id) {
        return execute(() -> rbacService.queryUserById(id));
    }

    @GetMapping("/users")
    public Response<RbacUserPageDTO> queryUserPage(@RequestParam(defaultValue = "1") Integer pageNum,
                                                   @RequestParam(defaultValue = "20") Integer pageSize,
                                                   @RequestParam(required = false) String username,
                                                   @RequestParam(required = false) Boolean status) {
        QueryRbacUserPageReq req = QueryRbacUserPageReq.builder()
                .pageNum(pageNum)
                .pageSize(pageSize)
                .username(username)
                .status(status)
                .build();
        return execute(() -> rbacService.queryUserPage(req));
    }

    @PutMapping("/users/{id}")
    public Response<RbacUserDTO> updateUser(@PathVariable("id") Long id, @RequestBody UpdateRbacUserReq req) {
        return execute(() -> {
            UpdateRbacUserReq updateReq = Objects.isNull(req) ? new UpdateRbacUserReq() : req;
            updateReq.setId(id);
            return rbacService.updateUser(updateReq);
        });
    }

    @DeleteMapping("/users/{id}")
    public Response<Boolean> deleteUser(@PathVariable("id") Long id) {
        return execute(() -> rbacService.deleteUser(DeleteRbacUserReq.builder().id(id).build()));
    }

    @PostMapping("/roles")
    public Response<RbacRoleDTO> createRole(@RequestBody CreateRbacRoleReq req) {
        return execute(() -> rbacService.createRole(req));
    }

    @GetMapping("/roles/{id}")
    public Response<RbacRoleDTO> queryRoleById(@PathVariable("id") Long id) {
        return execute(() -> rbacService.queryRoleById(id));
    }

    @GetMapping("/roles")
    public Response<RbacRolePageDTO> queryRolePage(@RequestParam(defaultValue = "1") Integer pageNum,
                                                   @RequestParam(defaultValue = "20") Integer pageSize,
                                                   @RequestParam(required = false) String roleCode,
                                                   @RequestParam(required = false) String roleName,
                                                   @RequestParam(required = false) Boolean status) {
        QueryRbacRolePageReq req = QueryRbacRolePageReq.builder()
                .pageNum(pageNum)
                .pageSize(pageSize)
                .roleCode(roleCode)
                .roleName(roleName)
                .status(status)
                .build();
        return execute(() -> rbacService.queryRolePage(req));
    }

    @PutMapping("/roles/{id}")
    public Response<RbacRoleDTO> updateRole(@PathVariable("id") Long id, @RequestBody UpdateRbacRoleReq req) {
        return execute(() -> {
            UpdateRbacRoleReq updateReq = Objects.isNull(req) ? new UpdateRbacRoleReq() : req;
            updateReq.setId(id);
            return rbacService.updateRole(updateReq);
        });
    }

    @DeleteMapping("/roles/{id}")
    public Response<Boolean> deleteRole(@PathVariable("id") Long id) {
        return execute(() -> rbacService.deleteRole(DeleteRbacRoleReq.builder().id(id).build()));
    }

    @PostMapping("/permissions")
    public Response<RbacPermissionDTO> createPermission(@RequestBody CreateRbacPermissionReq req) {
        return execute(() -> rbacService.createPermission(req));
    }

    @GetMapping("/permissions/{id}")
    public Response<RbacPermissionDTO> queryPermissionById(@PathVariable("id") Long id) {
        return execute(() -> rbacService.queryPermissionById(id));
    }

    @GetMapping("/permissions")
    public Response<RbacPermissionPageDTO> queryPermissionPage(@RequestParam(defaultValue = "1") Integer pageNum,
                                                               @RequestParam(defaultValue = "20") Integer pageSize,
                                                               @RequestParam(required = false) String permCode,
                                                               @RequestParam(required = false) String permName,
                                                               @RequestParam(required = false) Integer permType,
                                                               @RequestParam(required = false) Long parentId,
                                                               @RequestParam(required = false) Boolean status) {
        QueryRbacPermissionPageReq req = QueryRbacPermissionPageReq.builder()
                .pageNum(pageNum)
                .pageSize(pageSize)
                .permCode(permCode)
                .permName(permName)
                .permType(permType)
                .parentId(parentId)
                .status(status)
                .build();
        return execute(() -> rbacService.queryPermissionPage(req));
    }

    @PutMapping("/permissions/{id}")
    public Response<RbacPermissionDTO> updatePermission(@PathVariable("id") Long id, @RequestBody UpdateRbacPermissionReq req) {
        return execute(() -> {
            UpdateRbacPermissionReq updateReq = Objects.isNull(req) ? new UpdateRbacPermissionReq() : req;
            updateReq.setId(id);
            return rbacService.updatePermission(updateReq);
        });
    }

    @DeleteMapping("/permissions/{id}")
    public Response<Boolean> deletePermission(@PathVariable("id") Long id) {
        return execute(() -> rbacService.deletePermission(DeleteRbacPermissionReq.builder().id(id).build()));
    }

    @PutMapping("/users/{userId}/roles")
    public Response<Boolean> replaceUserRoles(@PathVariable("userId") Long userId,
                                              @RequestBody(required = false) ReplaceUserRolesReq req) {
        return execute(() -> {
            ReplaceUserRolesReq replaceReq = Objects.isNull(req) ? new ReplaceUserRolesReq() : req;
            replaceReq.setUserId(userId);
            return rbacService.replaceUserRoles(replaceReq);
        });
    }

    @GetMapping("/users/{userId}/roles")
    public Response<QueryUserRoleIdsResp> queryUserRoleIds(@PathVariable("userId") Long userId) {
        return execute(() -> rbacService.queryUserRoleIds(QueryUserRoleIdsReq.builder().userId(userId).build()));
    }

    @PutMapping("/roles/{roleId}/permissions")
    public Response<Boolean> replaceRolePermissions(@PathVariable("roleId") Long roleId,
                                                    @RequestBody(required = false) ReplaceRolePermissionsReq req) {
        return execute(() -> {
            ReplaceRolePermissionsReq replaceReq = Objects.isNull(req) ? new ReplaceRolePermissionsReq() : req;
            replaceReq.setRoleId(roleId);
            return rbacService.replaceRolePermissions(replaceReq);
        });
    }

    @GetMapping("/roles/{roleId}/permissions")
    public Response<QueryRolePermissionIdsResp> queryRolePermissionIds(@PathVariable("roleId") Long roleId) {
        return execute(() -> rbacService.queryRolePermissionIds(QueryRolePermissionIdsReq.builder().roleId(roleId).build()));
    }

    private <T> Response<T> execute(Supplier<T> supplier) {
        try {
            return success(supplier.get());
        } catch (AppException appException) {
            log.warn("RBAC 接口业务异常：code={}, info={}", appException.getCode(), appException.getInfo());
            return Response.<T>builder()
                    .code(appException.getCode())
                    .info(Objects.isNull(appException.getInfo()) ? Constants.ResponseCode.UN_ERROR.getInfo() : appException.getInfo())
                    .build();
        } catch (Exception exception) {
            log.error("RBAC 接口系统异常", exception);
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
