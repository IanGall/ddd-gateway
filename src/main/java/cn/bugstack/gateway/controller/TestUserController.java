package cn.bugstack.gateway.controller;

import cn.bugstack.api.IUserService;
import cn.bugstack.gateway.model.UserTestResponse;
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
    public UserTestResponse testUser(@RequestParam(defaultValue = "test") String req) {
        String rpcResult = userService.queryUserInfo(req);
        return new UserTestResponse(
                "0000",
                "success",
                req,
                rpcResult,
                System.currentTimeMillis()
        );
    }

}
