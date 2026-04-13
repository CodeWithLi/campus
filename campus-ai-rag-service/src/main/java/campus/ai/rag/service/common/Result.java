package campus.ai.rag.service.common;

import java.io.Serial;
import java.io.Serializable;

/**
 * 统一返回结构：用于所有 Controller 的返回，保证前后端交互格式一致。
 *
 * @param <T> 返回数据类型
 */
public class Result<T> implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private int code;
    private String msg;
    private T data;

    public Result() {
    }

    public Result(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(StatusCodeEnum.SUCCESS.getCode(), StatusCodeEnum.SUCCESS.getMessage(), data);
    }

    public static <T> Result<T> success() {
        return new Result<>(StatusCodeEnum.SUCCESS.getCode(), StatusCodeEnum.SUCCESS.getMessage(), null);
    }

    public static <T> Result<T> error(StatusCodeEnum statusCodeEnum) {
        return new Result<>(statusCodeEnum.getCode(), statusCodeEnum.getMessage(), null);
    }

    public static <T> Result<T> error(int code, String msg) {
        return new Result<>(code, msg, null);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}

