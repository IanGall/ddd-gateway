package ${package}.exception;

import cn.iantech.common.constant.Constants;
import cn.iantech.common.exception.AppException;
import cn.iantech.common.model.Response;
import ${package}.config.CachedBodyHttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static cn.iantech.common.constant.Constants.ResponseCode.INTERNAL_ERROR;
import static cn.iantech.common.constant.Constants.ResponseCode.INVALID_ARGUMENT;

@RestControllerAdvice
public class GatewayExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GatewayExceptionHandler.class);

    @ExceptionHandler(AppException.class)
    public ResponseEntity<Response<Void>> handleAppException(AppException exception) {
        Constants.ResponseCode responseCode = Constants.ResponseCode.fromCode(exception.getCode());
        if (responseCode == null) {
            return response(INTERNAL_ERROR);
        }
        String info = exception.getInfo() == null || exception.getInfo().isBlank()
                ? responseCode.getInfo() : exception.getInfo();
        return response(responseCode, info);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class,
            HttpMessageNotReadableException.class, MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Response<Void>> handleValidationException(Exception exception) {
        log.debug("网关请求参数校验失败: type={}", exception.getClass().getName());
        return response(INVALID_ARGUMENT);
    }

    @ExceptionHandler(CachedBodyHttpServletRequest.ChannelPayloadTooLargeException.class)
    public ResponseEntity<Response<Void>> handlePayloadTooLarge() {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(Response.<Void>builder().code("PAYLOAD_TOO_LARGE")
                        .info("渠道请求体不能超过 1 MiB").data(null).build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Response<Void>> handleException(Exception exception) {
        log.error("网关未处理异常", exception);
        return response(INTERNAL_ERROR);
    }

    private ResponseEntity<Response<Void>> response(Constants.ResponseCode responseCode) {
        return response(responseCode, responseCode.getInfo());
    }

    private ResponseEntity<Response<Void>> response(Constants.ResponseCode responseCode, String info) {
        return ResponseEntity.status(HttpStatus.valueOf(responseCode.getHttpStatus()))
                .body(Response.<Void>builder().code(responseCode.getCode()).info(info).data(null).build());
    }

}
