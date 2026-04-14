package campus.ai.rag.service.dto.wx;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "WxLoginDTO", description = "微信端登录请求参数（策略模式）")
public class WxLoginDTO {

    @ApiModelProperty(value = "登录方式：PASSWORD(用户名密码) / WX_ONE_CLICK(微信一键登录)", required = true, example = "PASSWORD")
    private String loginType;

    @ApiModelProperty(value = "用户名（PASSWORD方式必填）", example = "wx_user_001")
    private String username;

    @ApiModelProperty(value = "密码（PASSWORD方式必填）", example = "User@123456")
    private String password;

    @ApiModelProperty(value = "微信临时code（WX_ONE_CLICK方式必填）", example = "021Xxxxxxx")
    private String wxCode;
}

