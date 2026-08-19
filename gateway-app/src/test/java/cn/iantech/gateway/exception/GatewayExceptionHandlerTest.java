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

    // 验证授权拒绝映射为 403 响应
    @Test
    void shouldMapAccessDeniedToForbidden() {
        ResponseEntity<Response<Void>> response = handler.handleAppException(
                new AppException(Constants.ResponseCode.ACCESS_DENIED.getCode(), "无权访问"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(Constants.ResponseCode.ACCESS_DENIED.getCode(), response.getBody().getCode());
    }

    @Test
    void shouldMapAuthUnavailableToServiceUnavailable() {
        ResponseEntity<Response<Void>> response = handler.handleAppException(
                new AppException(Constants.ResponseCode.AUTH_UNAVAILABLE.getCode(),
                        Constants.ResponseCode.AUTH_UNAVAILABLE.getInfo()));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals(Constants.ResponseCode.AUTH_UNAVAILABLE.getCode(), response.getBody().getCode());
    }

    @Test
    void shouldMapAuthRateLimitedToTooManyRequests() {
        ResponseEntity<Response<Void>> response = handler.handleAppException(
                new AppException(Constants.ResponseCode.AUTH_RATE_LIMITED.getCode()));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertEquals(Constants.ResponseCode.AUTH_RATE_LIMITED.getCode(), response.getBody().getCode());
    }

    // 验证未由 Auth 客户端归一化的 Dubbo 异常返回 502
    @Test
    void shouldMapRpcExceptionToBadGateway() {
        ResponseEntity<Response<Void>> response = handler.handleRpcException(
                new RpcException(RpcException.BIZ_EXCEPTION, "认证失败",
                        new AppException("AUTH_REQUIRED", "令牌已过期")));

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        assertEquals(Constants.ResponseCode.RPC_ERROR.getCode(), response.getBody().getCode());
    }

    // 验证 Dubbo 超时异常映射为 504 响应
    @Test
    void shouldMapDubboTimeoutToGatewayTimeout() {
        ResponseEntity<Response<Void>> response = handler.handleRpcException(
                new RpcException(RpcException.TIMEOUT_EXCEPTION, "timeout"));

        assertEquals(HttpStatus.GATEWAY_TIMEOUT, response.getStatusCode());
        assertEquals("RPC_TIMEOUT", response.getBody().getCode());
    }

    // 验证普通 Dubbo 调用失败映射为 502 响应
    @Test
    void shouldMapDubboFailureToBadGateway() {
        ResponseEntity<Response<Void>> response = handler.handleRpcException(
                new RpcException(RpcException.NETWORK_EXCEPTION, "下游连接失败"));

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        assertEquals("RPC_ERROR", response.getBody().getCode());
        assertEquals("下游服务调用失败", response.getBody().getInfo());
    }

    // 验证参数异常映射为 400 响应
    @Test
    void shouldMapValidationExceptionToBadRequest() {
        ResponseEntity<Response<Void>> response = handler.handleValidationException(
                new jakarta.validation.ConstraintViolationException("参数错误", java.util.Set.of()));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(Constants.ResponseCode.INVALID_ARGUMENT.getCode(), response.getBody().getCode());
    }

}
