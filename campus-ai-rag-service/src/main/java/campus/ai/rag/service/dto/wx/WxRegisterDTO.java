package campus.ai.rag.service.dto.wx;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "WxRegisterDTO", description = "微信端注册请求参数")
public class WxRegisterDTO {

    @ApiModelProperty(value = "用户名", required = true, example = "wx_user_001")
    private String username;

    @ApiModelProperty(value = "密码（明文入参，后端BCrypt加密）", required = true, example = "User@123456")
    private String password;

    @ApiModelProperty(value = "邮箱", required = true, example = "test@example.com")
    private String email;

    @ApiModelProperty(value = "邮箱验证码", required = true, example = "A1B2C3")
    private String emailCode;

    @ApiModelProperty(value = "微信小程序临时登录code（用于换取openid）", required = true, example = "021Xxxxxxx")
    private String wxCode;
}

