package campus.ai.rag.service.exception;

import campus.ai.rag.service.common.StatusCodeEnum;

/**
 * 自定义业务异常：用于业务校验失败、业务规则不满足等情况。
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(String message) {
        super(message);
        this.code = StatusCodeEnum.BUSINESS_ERROR.getCode();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(StatusCodeEnum statusCodeEnum) {
        super(statusCodeEnum.getMessage());
        this.code = statusCodeEnum.getCode();
    }

    public BusinessException(StatusCodeEnum statusCodeEnum, String message) {
        super(message);
        this.code = statusCodeEnum.getCode();
    }

    public int getCode() {
        return code;
    }
}

