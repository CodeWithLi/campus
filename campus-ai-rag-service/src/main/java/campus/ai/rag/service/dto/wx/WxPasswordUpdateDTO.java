package campus.ai.rag.service.dto.wx;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("微信端修改密码")
public class WxPasswordUpdateDTO {

    @ApiModelProperty(value = "校验方式：EMAIL_CODE / WX_ONE_CLICK", required = true)
    private String verifyType;

    @ApiModelProperty(value = "邮箱验证码（EMAIL_CODE时必填）")
    private String emailCode;

    @ApiModelProperty(value = "微信wxCode（WX_ONE_CLICK时必填）")
    private String wxCode;

    @ApiModelProperty(value = "新密码", required = true)
    private String newPassword;
}
