package campus.ai.rag.service.dto.wx;

import lombok.Data;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 微信端账号密码登录参数。
 */
@Data
@ApiModel(value = "WxLoginDTO", description = "微信端登录请求参数")
public class WxLoginDTO {

    @ApiModelProperty(value = "用户名", required = true, example = "wx_user_001")
    private String username;
    @ApiModelProperty(value = "密码", required = true, example = "123456")
    private String password;
}

