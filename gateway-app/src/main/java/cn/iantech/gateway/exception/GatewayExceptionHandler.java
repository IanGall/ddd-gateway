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
        String message = Objects.requireNonNullElse(exception.getInfo(), Constants.ResponseCode.UN_ERROR.getInfo());
        if ("AUTH_REQUIRED".equals(exception.getCode()) || exception.getCode().startsWith("AUTH_REFRESH")) {
            return response(HttpStatus.UNAUTHORIZED, exception.getCode(), message);
        }
        if (Constants.ResponseCode.ACCESS_DENIED.getCode().equals(exception.getCode())
                || "ACCESS_DENIED".equals(exception.getCode())) {
            return response(HttpStatus.FORBIDDEN, exception.getCode(), message);
        }
        return response(HttpStatus.UNPROCESSABLE_ENTITY, exception.getCode(), message);
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
        return response(HttpStatus.BAD_REQUEST, Constants.ResponseCode.ILLEGAL_PARAMETER.getCode(),
                Objects.requireNonNullElse(message, Constants.ResponseCode.ILLEGAL_PARAMETER.getInfo()));
    }

    @ExceptionHandler(RpcException.class)
    public ResponseEntity<Response<Void>> handleRpcException(RpcException exception) {
        log.warn("网关调用下游 RPC 失败: timeout={}, noInvoker={}", exception.isTimeout(),
                exception.isNoInvokerAvailableAfterFilter());
        if (exception.isTimeout()) {
            return response(HttpStatus.GATEWAY_TIMEOUT, "RPC_TIMEOUT", "下游服务调用超时");
        }
        if (exception.isNoInvokerAvailableAfterFilter()) {
            return response(HttpStatus.SERVICE_UNAVAILABLE, "RPC_NO_PROVIDER", "下游服务暂无可用提供者");
        }
        return response(HttpStatus.BAD_GATEWAY, "RPC_ERROR", "下游服务调用失败");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Response<Void>> handleException(Exception exception) {
        log.error("网关未处理异常", exception);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, Constants.ResponseCode.UN_ERROR.getCode(),
                Constants.ResponseCode.UN_ERROR.getInfo());
    }

    private ResponseEntity<Response<Void>> response(HttpStatus status, String code, String info) {
        return ResponseEntity.status(status).body(Response.<Void>builder().code(code).info(info).build());
    }

}
