package cn.iantech.gateway.exception;

import cn.iantech.common.constant.Constants;
import cn.iantech.common.exception.AppException;
import cn.iantech.common.model.Response;
import jakarta.validation.ConstraintViolationException;
import org.apache.dubbo.rpc.RpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Objects;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GatewayExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GatewayExceptionHandler.class);

    @ExceptionHandler(AppException.class)
    public ResponseEntity<Response<Void>> handleAppException(AppException exception) {
        String code = Objects.requireNonNullElse(exception.getCode(), Constants.ResponseCode.INTERNAL_ERROR.getCode());
        String info = Objects.requireNonNullElse(exception.getInfo(), defaultInfo(code));
        return response(status(code), code, info);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class,
            HttpMessageNotReadableException.class, MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Response<Void>> handleValidationException(Exception exception) {
        String message = exception instanceof MethodArgumentNotValidException methodException
                ? methodException.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "))
                : exception.getMessage();
        return response(HttpStatus.BAD_REQUEST, Constants.ResponseCode.INVALID_ARGUMENT.getCode(),
                Objects.requireNonNullElse(message, Constants.ResponseCode.INVALID_ARGUMENT.getInfo()));
    }

    @ExceptionHandler(RpcException.class)
    public ResponseEntity<Response<Void>> handleRpcException(RpcException exception) {
        log.warn("网关调用下游 RPC 失败: timeout={}, noInvoker={}", exception.isTimeout(),
                exception.isNoInvokerAvailableAfterFilter());
        if (exception.isTimeout()) {
            return response(HttpStatus.GATEWAY_TIMEOUT, Constants.ResponseCode.RPC_TIMEOUT);
        }
        if (exception.isNoInvokerAvailableAfterFilter()) {
            return response(HttpStatus.SERVICE_UNAVAILABLE, Constants.ResponseCode.RPC_NO_PROVIDER);
        }
        return response(HttpStatus.BAD_GATEWAY, Constants.ResponseCode.RPC_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Response<Void>> handleException(Exception exception) {
        log.error("网关未处理异常", exception);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, Constants.ResponseCode.INTERNAL_ERROR);
    }

    private ResponseEntity<Response<Void>> response(HttpStatus status, String code, String info) {
        return ResponseEntity.status(status).body(Response.<Void>builder().code(code).info(info).build());
    }

    private ResponseEntity<Response<Void>> response(HttpStatus status, Constants.ResponseCode responseCode) {
        return response(status, responseCode.getCode(), responseCode.getInfo());
    }

    private HttpStatus status(String code) {
        if (Constants.ResponseCode.INVALID_ARGUMENT.getCode().equals(code)) {
            return HttpStatus.BAD_REQUEST;
        }
        if (Constants.ResponseCode.AUTH_REQUIRED.getCode().equals(code)) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (Constants.ResponseCode.ACCESS_DENIED.getCode().equals(code)) {
            return HttpStatus.FORBIDDEN;
        }
        if (Constants.ResponseCode.AUTH_REFRESH_BUSY.getCode().equals(code)) {
            return HttpStatus.CONFLICT;
        }
        if (Constants.ResponseCode.AUTH_UNAVAILABLE.getCode().equals(code)
                || Constants.ResponseCode.RPC_NO_PROVIDER.getCode().equals(code)) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }
        if (Constants.ResponseCode.RPC_TIMEOUT.getCode().equals(code)) {
            return HttpStatus.GATEWAY_TIMEOUT;
        }
        if (Constants.ResponseCode.RPC_ERROR.getCode().equals(code)) {
            return HttpStatus.BAD_GATEWAY;
        }
        if (Constants.ResponseCode.INTERNAL_ERROR.getCode().equals(code)) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return HttpStatus.UNPROCESSABLE_ENTITY;
    }

    private String defaultInfo(String code) {
        return java.util.Arrays.stream(Constants.ResponseCode.values())
                .filter(responseCode -> responseCode.getCode().equals(code))
                .map(Constants.ResponseCode::getInfo)
                .findFirst()
                .orElse(Constants.ResponseCode.INTERNAL_ERROR.getInfo());
    }

}
