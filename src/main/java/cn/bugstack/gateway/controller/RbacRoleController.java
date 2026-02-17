package cn.bugstack.gateway.controller;

import cn.bugstack.api.IRbacRoleService;
import cn.bugstack.api.model.rbac.CreateRbacRoleReq;
import cn.bugstack.api.model.rbac.DeleteRbacRoleReq;
import cn.bugstack.api.model.rbac.QueryRbacRolePageReq;
import cn.bugstack.api.model.rbac.RbacRoleDTO;
import cn.bugstack.api.model.rbac.RbacRolePageDTO;
import cn.bugstack.api.model.rbac.UpdateRbacRoleReq;
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
@RequestMapping("/api/rbac/roles")
public class RbacRoleController {

    @DubboReference(version = "1.0.0", timeout = 10000, retries = 0, check = false)
    private IRbacRoleService rbacRoleService;

    @PostMapping
    public Response<RbacRoleDTO> createRole(@RequestBody CreateRbacRoleReq req) {
        return execute(() -> rbacRoleService.createRole(req));
    }

    @GetMapping("/{id}")
    public Response<RbacRoleDTO> queryRoleById(@PathVariable("id") Long id) {
        return execute(() -> rbacRoleService.queryRoleById(id));
    }

    @GetMapping
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
        return execute(() -> rbacRoleService.queryRolePage(req));
    }

    @PutMapping("/{id}")
    public Response<RbacRoleDTO> updateRole(@PathVariable("id") Long id, @RequestBody UpdateRbacRoleReq req) {
        return execute(() -> {
            UpdateRbacRoleReq updateReq = Objects.isNull(req) ? new UpdateRbacRoleReq() : req;
            updateReq.setId(id);
            return rbacRoleService.updateRole(updateReq);
        });
    }

    @DeleteMapping("/{id}")
    public Response<Boolean> deleteRole(@PathVariable("id") Long id) {
        return execute(() -> rbacRoleService.deleteRole(DeleteRbacRoleReq.builder().id(id).build()));
    }

    private <T> Response<T> execute(Supplier<T> supplier) {
        try {
            return success(supplier.get());
        } catch (AppException appException) {
            log.warn("RBAC 角色接口业务异常：code={}, info={}", appException.getCode(), appException.getInfo());
            return Response.<T>builder()
                    .code(appException.getCode())
                    .info(Objects.isNull(appException.getInfo()) ? Constants.ResponseCode.UN_ERROR.getInfo() : appException.getInfo())
                    .build();
        } catch (Exception exception) {
            log.error("RBAC 角色接口系统异常", exception);
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
