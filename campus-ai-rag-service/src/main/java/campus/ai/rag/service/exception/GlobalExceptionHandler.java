package campus.ai.rag.service.exception;

import campus.ai.rag.service.common.Result;
import campus.ai.rag.service.common.StatusCodeEnum;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理：统一返回 Result 格式，避免业务代码中到处 try-catch。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String SSE_MIME = MediaType.TEXT_EVENT_STREAM_VALUE;

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Object>> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        log.info("BusinessException: code={}, msg={}", ex.getCode(), ex.getMessage());
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
        if (isSseRequest(request)) {
            builder.contentType(MediaType.APPLICATION_JSON);
        }
        return builder.body(Result.error(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Object>> handleNoResourceFoundException(NoResourceFoundException ex, HttpServletRequest request) {
        log.warn("静态资源不存在，path={}", ex.getResourcePath());
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(HttpStatus.NOT_FOUND);
        if (isSseRequest(request)) {
            builder.contentType(MediaType.APPLICATION_JSON);
        }
        return builder.body(Result.error(StatusCodeEnum.NOT_FOUND.getCode(), "资源不存在: " + ex.getResourcePath()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Object>> handleException(Exception ex, HttpServletRequest request) {
        log.error("系统异常，错误信息={}", ex.getMessage(), ex);
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR);
        if (isSseRequest(request)) {
            builder.contentType(MediaType.APPLICATION_JSON);
        }
        return builder.body(Result.error(StatusCodeEnum.INTERNAL_SERVER_ERROR));
    }

    private boolean isSseRequest(HttpServletRequest request) {
        if (request == null) return false;
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains(SSE_MIME)) {
            return true;
        }
        String uri = request.getRequestURI();
        return uri != null && uri.endsWith("/stream");
    }
}

