package cn.bugstack.gateway.controller;

import cn.bugstack.api.IRbacPermissionService;
import cn.bugstack.api.model.rbac.CreateRbacPermissionReq;
import cn.bugstack.api.model.rbac.DeleteRbacPermissionReq;
import cn.bugstack.api.model.rbac.QueryRbacPermissionPageReq;
import cn.bugstack.api.model.rbac.RbacPermissionDTO;
import cn.bugstack.api.model.rbac.RbacPermissionPageDTO;
import cn.bugstack.api.model.rbac.UpdateRbacPermissionReq;
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
@RequestMapping("/api/rbac/permissions")
public class RbacPermissionController {

    @DubboReference(version = "1.0.0", timeout = 10000, retries = 0, check = false)
    private IRbacPermissionService rbacPermissionService;

    @PostMapping
    public Response<RbacPermissionDTO> createPermission(@RequestBody CreateRbacPermissionReq req) {
        return execute(() -> rbacPermissionService.createPermission(req));
    }

    @GetMapping("/{id}")
    public Response<RbacPermissionDTO> queryPermissionById(@PathVariable("id") Long id) {
        return execute(() -> rbacPermissionService.queryPermissionById(id));
    }

    @GetMapping
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
        return execute(() -> rbacPermissionService.queryPermissionPage(req));
    }

    @PutMapping("/{id}")
    public Response<RbacPermissionDTO> updatePermission(@PathVariable("id") Long id, @RequestBody UpdateRbacPermissionReq req) {
        return execute(() -> {
            UpdateRbacPermissionReq updateReq = Objects.isNull(req) ? new UpdateRbacPermissionReq() : req;
            updateReq.setId(id);
            return rbacPermissionService.updatePermission(updateReq);
        });
    }

    @DeleteMapping("/{id}")
    public Response<Boolean> deletePermission(@PathVariable("id") Long id) {
        return execute(() -> rbacPermissionService.deletePermission(DeleteRbacPermissionReq.builder().id(id).build()));
    }

    private <T> Response<T> execute(Supplier<T> supplier) {
        try {
            return success(supplier.get());
        } catch (AppException appException) {
            log.warn("RBAC 权限接口业务异常：code={}, info={}", appException.getCode(), appException.getInfo());
            return Response.<T>builder()
                    .code(appException.getCode())
                    .info(Objects.isNull(appException.getInfo()) ? Constants.ResponseCode.UN_ERROR.getInfo() : appException.getInfo())
                    .build();
        } catch (Exception exception) {
            log.error("RBAC 权限接口系统异常", exception);
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
