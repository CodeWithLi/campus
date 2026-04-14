package campus.ai.rag.service.dto.admin.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "AdminUserResetPasswordDTO", description = "后台重置管理员密码请求参数")
public class AdminUserResetPasswordDTO {

    @ApiModelProperty(value = "新密码（明文入参，后端BCrypt加密）", required = true, example = "Admin@123456")
    private String newPassword;
}

