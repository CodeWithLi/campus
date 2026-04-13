package campus.ai.rag.service.common;

/**
 * 统一状态码枚举，避免业务代码中出现魔法数字。
 */
public enum StatusCodeEnum {
    SUCCESS(200, "success"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "资源冲突"),
    BUSINESS_ERROR(422, "业务处理失败"),
    TOO_MANY_REQUESTS(429, "请求过于频繁"),
    INTERNAL_SERVER_ERROR(500, "系统繁忙，请稍后再试");

    private final int code;
    private final String message;

    StatusCodeEnum(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}

