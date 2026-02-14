package cn.bugstack.gateway.controller;

import cn.bugstack.api.IRbacUserService;
import cn.bugstack.api.model.rbac.CreateRbacUserReq;
import cn.bugstack.api.model.rbac.DeleteRbacUserReq;
import cn.bugstack.api.model.rbac.QueryRbacUserPageReq;
import cn.bugstack.api.model.rbac.RbacUserDTO;
import cn.bugstack.api.model.rbac.RbacUserPageDTO;
import cn.bugstack.api.model.rbac.UpdateRbacUserReq;
import cn.bugstack.types.common.Constants;
import cn.bugstack.types.exception.AppException;
import cn.bugstack.types.model.Response;
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
@RequestMapping("/api/rbac/users")
public class RbacUserController {

    @DubboReference(version = "1.0.0", timeout = 10000, retries = 0, check = false)
    private IRbacUserService rbacUserService;

    @PostMapping
    public Response<RbacUserDTO> createUser(@RequestBody CreateRbacUserReq req) {
        return execute(() -> rbacUserService.createUser(req));
    }

    @GetMapping("/{id}")
    public Response<RbacUserDTO> queryUserById(@PathVariable("id") Long id) {
        return execute(() -> rbacUserService.queryUserById(id));
    }

    @GetMapping
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
        return execute(() -> rbacUserService.queryUserPage(req));
    }

    @PutMapping("/{id}")
    public Response<RbacUserDTO> updateUser(@PathVariable("id") Long id, @RequestBody UpdateRbacUserReq req) {
        return execute(() -> {
            UpdateRbacUserReq updateReq = Objects.isNull(req) ? new UpdateRbacUserReq() : req;
            updateReq.setId(id);
            return rbacUserService.updateUser(updateReq);
        });
    }

    @DeleteMapping("/{id}")
    public Response<Boolean> deleteUser(@PathVariable("id") Long id) {
        return execute(() -> rbacUserService.deleteUser(DeleteRbacUserReq.builder().id(id).build()));
    }

    private <T> Response<T> execute(Supplier<T> supplier) {
        try {
            return success(supplier.get());
        } catch (AppException appException) {
            log.warn("RBAC 用户接口业务异常：code={}, info={}", appException.getCode(), appException.getInfo());
            return Response.<T>builder()
                    .code(appException.getCode())
                    .info(Objects.isNull(appException.getInfo()) ? Constants.ResponseCode.UN_ERROR.getInfo() : appException.getInfo())
                    .build();
        } catch (Exception exception) {
            log.error("RBAC 用户接口系统异常", exception);
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
