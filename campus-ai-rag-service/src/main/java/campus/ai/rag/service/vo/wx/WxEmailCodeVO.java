package campus.ai.rag.service.vo.wx;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "WxEmailCodeVO", description = "微信端获取邮箱验证码返回参数")
public class WxEmailCodeVO {

    @ApiModelProperty(value = "邮箱", example = "test@example.com")
    private String email;

    @ApiModelProperty(value = "验证码（当前阶段用于联调，后续可移除）", example = "A1B2C3")
    private String code;

    @ApiModelProperty(value = "有效期（秒）", example = "300")
    private long expireSeconds;
}

