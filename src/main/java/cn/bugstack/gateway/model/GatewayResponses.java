package cn.bugstack.gateway.model;

import cn.bugstack.common.constant.Constants;
import cn.bugstack.common.model.Response;

public final class GatewayResponses {

    private GatewayResponses() { }

    public static <T> Response<T> success(T data) {
        return Response.<T>builder()
                .code(Constants.ResponseCode.SUCCESS.getCode())
                .info(Constants.ResponseCode.SUCCESS.getInfo())
                .data(data)
                .build();
    }

}
