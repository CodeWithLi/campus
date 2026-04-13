package campus.ai.rag.service.dto.admin;

import lombok.Data;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 后台登录参数。
 */
@Data
@ApiModel(value = "AdminLoginDTO", description = "后台登录请求参数")
public class AdminLoginDTO {

    @ApiModelProperty(value = "用户名", required = true, example = "superadmin")
    private String username;
    @ApiModelProperty(value = "密码", required = true, example = "superadmin")
    private String password;
}

