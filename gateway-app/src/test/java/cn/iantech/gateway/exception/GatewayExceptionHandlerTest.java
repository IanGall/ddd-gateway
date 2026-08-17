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

    // 验证业务异常映射为 422 响应
    @Test
    void shouldMapBusinessExceptionToUnprocessableEntity() {
        ResponseEntity<Response<Void>> response = handler.handleAppException(new AppException("USER_EXISTS", "用户已存在"));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertEquals("USER_EXISTS", response.getBody().getCode());
    }

    // 验证 Dubbo 超时异常映射为 504 响应
    @Test
    void shouldMapDubboTimeoutToGatewayTimeout() {
        ResponseEntity<Response<Void>> response = handler.handleRpcException(
                new RpcException(RpcException.TIMEOUT_EXCEPTION, "timeout"));

        assertEquals(HttpStatus.GATEWAY_TIMEOUT, response.getStatusCode());
        assertEquals("RPC_TIMEOUT", response.getBody().getCode());
    }

    // 验证参数异常映射为 400 响应
    @Test
    void shouldMapValidationExceptionToBadRequest() {
        ResponseEntity<Response<Void>> response = handler.handleValidationException(
                new jakarta.validation.ConstraintViolationException("参数错误", java.util.Set.of()));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(Constants.ResponseCode.ILLEGAL_PARAMETER.getCode(), response.getBody().getCode());
    }

}
