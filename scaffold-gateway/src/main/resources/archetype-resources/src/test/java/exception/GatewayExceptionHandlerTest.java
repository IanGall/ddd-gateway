package ${package}.exception;

import cn.iantech.common.constant.Constants;
import cn.iantech.common.exception.AppException;
import cn.iantech.common.model.Response;
import jakarta.validation.ConstraintViolationException;
import org.apache.dubbo.rpc.RpcException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GatewayExceptionHandlerTest {

    private final GatewayExceptionHandler handler = new GatewayExceptionHandler();

    @Test
    void shouldMapAuthRequiredToUnauthorized() {
        ResponseEntity<Response<Void>> response = handler.handleAppException(
                new AppException(Constants.ResponseCode.AUTH_REQUIRED.getCode(), "令牌已过期"));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(Constants.ResponseCode.AUTH_REQUIRED.getCode(), response.getBody().getCode());
        assertNull(response.getBody().getData());
    }

    @Test
    void shouldMapRefreshBusyToConflict() {
        ResponseEntity<Response<Void>> response = handler.handleAppException(
                new AppException(Constants.ResponseCode.AUTH_REFRESH_BUSY.getCode()));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(Constants.ResponseCode.AUTH_REFRESH_BUSY.getInfo(), response.getBody().getInfo());
    }

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
                new AppException(Constants.ResponseCode.AUTH_UNAVAILABLE.getCode()));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals(Constants.ResponseCode.AUTH_UNAVAILABLE.getCode(), response.getBody().getCode());
    }

    @Test
    void shouldMapValidationToBadRequest() {
        ResponseEntity<Response<Void>> response = handler.handleValidationException(
                new ConstraintViolationException("参数错误", Set.of()));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(Constants.ResponseCode.INVALID_ARGUMENT.getCode(), response.getBody().getCode());
    }

    @Test
    void shouldMapDubboTimeoutToGatewayTimeout() {
        ResponseEntity<Response<Void>> response = handler.handleRpcException(
                new RpcException(RpcException.TIMEOUT_EXCEPTION, "timeout"));

        assertEquals(HttpStatus.GATEWAY_TIMEOUT, response.getStatusCode());
        assertEquals(Constants.ResponseCode.RPC_TIMEOUT.getCode(), response.getBody().getCode());
    }

    @Test
    void shouldMapDubboFailureToBadGateway() {
        ResponseEntity<Response<Void>> response = handler.handleRpcException(
                new RpcException(RpcException.NETWORK_EXCEPTION, "下游连接失败"));

        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        assertEquals(Constants.ResponseCode.RPC_ERROR.getCode(), response.getBody().getCode());
        assertNull(response.getBody().getData());
    }
}
