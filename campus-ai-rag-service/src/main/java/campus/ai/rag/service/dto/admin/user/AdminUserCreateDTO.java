package campus.ai.rag.service.dto.admin.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "AdminUserCreateDTO", description = "后台新增管理员请求参数")
public class AdminUserCreateDTO {

    @ApiModelProperty(value = "管理员用户名（唯一）", required = true, example = "ops_admin")
    private String username;

    @ApiModelProperty(value = "管理员登录密码（明文入参，后端BCrypt加密）", required = true, example = "Admin@123456")
    private String password;

    @ApiModelProperty(value = "管理员昵称", required = true, example = "运营管理员")
    private String nickname;

    @ApiModelProperty(value = "状态：1=启用，0=禁用", example = "1")
    private Integer status;

    @ApiModelProperty(value = "角色ID（rbac_role.id）", required = true, example = "2")
    private Long roleId;
}

