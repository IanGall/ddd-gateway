package ${package}.model;

import cn.iantech.common.constant.Constants;
import cn.iantech.common.model.Response;

/** 网关统一成功响应构造器。 */
public final class GatewayResponses {

    private GatewayResponses() {
    }

    public static <T> Response<T> success(T data) {
        return Response.<T>builder()
                .code(Constants.ResponseCode.SUCCESS.getCode())
                .info(Constants.ResponseCode.SUCCESS.getInfo())
                .data(data)
                .build();
    }
}
