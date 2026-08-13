package cn.iantech.gateway.exception;

import cn.iantech.common.constant.Constants;
import cn.iantech.common.exception.AppException;
import cn.iantech.common.model.Response;
import org.apache.dubbo.rpc.RpcException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GatewayExceptionHandlerTest {

    private final GatewayExceptionHandler handler = new GatewayExceptionHandler();

    @Test
    void 应将业务异常映射为422() {
        ResponseEntity<Response<Void>> response = handler.handleAppException(new AppException("USER_EXISTS", "用户已存在"));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertEquals("USER_EXISTS", response.getBody().getCode());
    }

    @Test
    void 应将Dubbo超时映射为504() {
        ResponseEntity<Response<Void>> response = handler.handleRpcException(
                new RpcException(RpcException.TIMEOUT_EXCEPTION, "timeout"));

        assertEquals(HttpStatus.GATEWAY_TIMEOUT, response.getStatusCode());
        assertEquals("RPC_TIMEOUT", response.getBody().getCode());
    }

    @Test
    void 应将参数异常映射为400() {
        ResponseEntity<Response<Void>> response = handler.handleValidationException(
                new jakarta.validation.ConstraintViolationException("参数错误", java.util.Set.of()));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(Constants.ResponseCode.ILLEGAL_PARAMETER.getCode(), response.getBody().getCode());
    }

}
