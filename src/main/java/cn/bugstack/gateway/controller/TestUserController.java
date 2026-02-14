package cn.bugstack.gateway.controller;

import cn.bugstack.api.IUserService;
import cn.bugstack.types.common.Constants;
import cn.bugstack.types.model.Response;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestUserController {

    @DubboReference(version = "1.0.0", timeout = 3000, check = false)
    private IUserService userService;

    @GetMapping("/user")
    public Response<String> testUser(@RequestParam(defaultValue = "test") String req) {
        String rpcResult = userService.queryUserInfo(req);
        return Response.<String>builder()
                .code(Constants.ResponseCode.SUCCESS.getCode())
                .info(Constants.ResponseCode.SUCCESS.getInfo())
                .data(rpcResult)
                .build();
    }

}
