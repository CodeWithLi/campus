package campus.ai.rag.service.vo.admin.user;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel(value = "AdminUserVO", description = "后台管理员返回参数")
public class AdminUserVO {

    @ApiModelProperty(value = "管理员ID", example = "1")
    private Long id;

    @ApiModelProperty(value = "管理员用户名", example = "super_admin")
    private String username;

    @ApiModelProperty(value = "管理员昵称", example = "超级管理员")
    private String nickname;

    @ApiModelProperty(value = "状态：1=启用，0=禁用", example = "1")
    private Integer status;

    @ApiModelProperty(value = "是否超级管理员标识（Y/N）", example = "Y")
    private String superAdminMarker;

    @ApiModelProperty(value = "角色ID", example = "1")
    private Long roleId;

    @ApiModelProperty(value = "角色编码", example = "SUPER_ADMIN")
    private String roleCode;

    @ApiModelProperty(value = "角色名称", example = "超级管理员")
    private String roleName;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createdAt;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updatedAt;
}

