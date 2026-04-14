package campus.ai.rag.service.dto.wx;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "WxEmailCodeDTO", description = "微信端获取邮箱验证码请求参数")
public class WxEmailCodeDTO {

    @ApiModelProperty(value = "邮箱", required = true, example = "test@example.com")
    private String email;
}

