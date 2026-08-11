package cn.bugstack.gateway.controller;

import cn.bugstack.api.IRbacService;
import cn.bugstack.api.model.rbac.CreateRbacRoleReq;
import cn.bugstack.api.model.rbac.DeleteRbacRoleReq;
import cn.bugstack.api.model.rbac.QueryRbacRolePageReq;
import cn.bugstack.api.model.rbac.RbacRoleDTO;
import cn.bugstack.api.model.rbac.RbacRolePageDTO;
import cn.bugstack.api.model.rbac.UpdateRbacRoleReq;
import cn.bugstack.common.model.Response;
import cn.bugstack.gateway.model.RbacWebRequests;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.bugstack.gateway.model.GatewayResponses.success;

@Validated
@RestController
@RequestMapping("/api/rbac/roles")
public class RbacRoleController {

    @DubboReference(version = "1.0.0", timeout = 10000, retries = 0, check = false)
    private IRbacService rbacService;

    @PostMapping
    public Response<RbacRoleDTO> createRole(@Valid @RequestBody RbacWebRequests.CreateRole request) {
        CreateRbacRoleReq rpcRequest = CreateRbacRoleReq.builder()
                .roleCode(request.roleCode())
                .roleName(request.roleName())
                .roleDesc(request.roleDesc())
                .status(request.status())
                .build();
        return success(rbacService.createRole(rpcRequest));
    }

    @GetMapping("/{id}")
    public Response<RbacRoleDTO> queryRoleById(@Positive(message = "角色ID必须大于0") @PathVariable("id") Long id) {
        return success(rbacService.queryRoleById(id));
    }

    @GetMapping
    public Response<RbacRolePageDTO> queryRolePage(
            @Min(value = 1, message = "页码必须大于0") @RequestParam(defaultValue = "1") Integer pageNum,
            @Min(value = 1, message = "页大小必须大于0") @Max(value = 100, message = "页大小不能超过100")
            @RequestParam(defaultValue = "20") Integer pageSize,
            @Size(max = 64, message = "角色编码长度不能超过64") @RequestParam(required = false) String roleCode,
            @Size(max = 128, message = "角色名称长度不能超过128") @RequestParam(required = false) String roleName,
            @RequestParam(required = false) Boolean status) {
        QueryRbacRolePageReq rpcRequest = QueryRbacRolePageReq.builder()
                .pageNum(pageNum)
                .pageSize(pageSize)
                .roleCode(roleCode)
                .roleName(roleName)
                .status(status)
                .build();
        return success(rbacService.queryRolePage(rpcRequest));
    }

    @PutMapping("/{id}")
    public Response<RbacRoleDTO> updateRole(
            @Positive(message = "角色ID必须大于0") @PathVariable("id") Long id,
            @Valid @RequestBody RbacWebRequests.UpdateRole request) {
        UpdateRbacRoleReq rpcRequest = UpdateRbacRoleReq.builder()
                .id(id)
                .roleCode(request.roleCode())
                .roleName(request.roleName())
                .roleDesc(request.roleDesc())
                .status(request.status())
                .build();
        return success(rbacService.updateRole(rpcRequest));
    }

    @DeleteMapping("/{id}")
    public Response<Boolean> deleteRole(@Positive(message = "角色ID必须大于0") @PathVariable("id") Long id) {
        return success(rbacService.deleteRole(DeleteRbacRoleReq.builder().id(id).build()));
    }

}
