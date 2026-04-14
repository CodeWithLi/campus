package campus.ai.rag.service.dto.admin.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "AdminUserUpdateDTO", description = "后台更新管理员请求参数")
public class AdminUserUpdateDTO {

    @ApiModelProperty(value = "管理员昵称", example = "内容运营管理员")
    private String nickname;

    @ApiModelProperty(value = "状态：1=启用，0=禁用", example = "1")
    private Integer status;

    @ApiModelProperty(value = "角色ID（rbac_role.id）", example = "2")
    private Long roleId;
}

