package campus.ai.rag.service.vo.admin;

import lombok.Data;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 后台登录返回。
 */
@Data
@ApiModel(value = "AdminLoginVO", description = "登录返回参数")
public class AdminLoginVO {

    @ApiModelProperty(value = "登录令牌", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String token;
    @ApiModelProperty(value = "Token请求头名称", example = "Authorization")
    private String tokenName;
    @ApiModelProperty(value = "过期时间（毫秒）", example = "7200000")
    private long expiresIn;
}

