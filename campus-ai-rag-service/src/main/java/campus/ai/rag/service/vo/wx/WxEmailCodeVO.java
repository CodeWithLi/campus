package campus.ai.rag.service.vo.wx;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "WxEmailCodeVO", description = "微信端获取邮箱验证码返回参数")
public class WxEmailCodeVO {

    @ApiModelProperty(value = "邮箱", example = "test@example.com")
    private String email;

    @ApiModelProperty(value = "发送结果提示", example = "验证码发送成功，请注意查收邮箱")
    private String message;

    @ApiModelProperty(value = "有效期（秒）", example = "300")
    private long expireSeconds;
}

