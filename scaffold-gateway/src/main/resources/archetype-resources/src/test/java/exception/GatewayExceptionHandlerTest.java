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
    void shouldMapAuthRateLimitedToTooManyRequests() {
        ResponseEntity<Response<Void>> response = handler.handleAppException(
                new AppException(Constants.ResponseCode.AUTH_RATE_LIMITED.getCode()));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertEquals(Constants.ResponseCode.AUTH_RATE_LIMITED.getCode(), response.getBody().getCode());
    }

    @Test
    void shouldMapUnknownCodeToInternalError() {
        ResponseEntity<Response<Void>> response = handler.handleAppException(new AppException("UNKNOWN", "业务异常"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(Constants.ResponseCode.INTERNAL_ERROR.getCode(), response.getBody().getCode());
        assertEquals(Constants.ResponseCode.INTERNAL_ERROR.getInfo(), response.getBody().getInfo());
    }

    @Test
    void shouldMapEmptyCodeToInternalError() {
        ResponseEntity<Response<Void>> response = handler.handleAppException(new AppException("", "业务异常"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(Constants.ResponseCode.INTERNAL_ERROR.getCode(), response.getBody().getCode());
    }

    @Test
    void shouldMapNullCodeToInternalError() {
        ResponseEntity<Response<Void>> response = handler.handleAppException(new AppException(null, "业务异常"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(Constants.ResponseCode.INTERNAL_ERROR.getCode(), response.getBody().getCode());
    }

    // 参数校验固定返回协议文案，不透传底层解析异常消息。
    @Test
    void shouldMapValidationToBadRequest() {
        ResponseEntity<Response<Void>> response = handler.handleValidationException(
                new ConstraintViolationException("参数错误", Set.of()));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(Constants.ResponseCode.INVALID_ARGUMENT.getCode(), response.getBody().getCode());
        assertEquals(Constants.ResponseCode.INVALID_ARGUMENT.getInfo(), response.getBody().getInfo());
    }

    @Test
    void shouldMapRawRpcTimeoutThroughSharedTranslator() {
        ResponseEntity<Response<Void>> response = handler.handleRpcException(
                new RpcException(RpcException.TIMEOUT_EXCEPTION, "timeout"));

        assertEquals(HttpStatus.GATEWAY_TIMEOUT, response.getStatusCode());
        assertEquals(Constants.ResponseCode.RPC_TIMEOUT.getCode(), response.getBody().getCode());
    }
}
