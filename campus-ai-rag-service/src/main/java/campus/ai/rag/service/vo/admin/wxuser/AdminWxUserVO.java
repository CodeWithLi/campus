package campus.ai.rag.service.vo.admin.wxuser;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel(value = "AdminWxUserVO", description = "后台微信用户返回参数")
public class AdminWxUserVO {

    @ApiModelProperty(value = "用户ID", example = "1")
    private Long id;

    @ApiModelProperty(value = "用户名", example = "wx_user_001")
    private String username;

    @ApiModelProperty(value = "邮箱", example = "test@example.com")
    private String email;

    @ApiModelProperty(value = "昵称", example = "校园小助手用户")
    private String nickname;

    @ApiModelProperty(value = "头像URL")
    private String avatarUrl;

    @ApiModelProperty(value = "用户状态：1=正常，0=冻结", example = "1")
    private Integer status;

    @ApiModelProperty(value = "注册来源：PASSWORD/WX", example = "PASSWORD")
    private String registerSource;

    @ApiModelProperty(value = "剩余算力点", example = "120")
    private Integer powerPoints;

    @ApiModelProperty(value = "最后登录时间")
    private LocalDateTime lastLoginAt;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createdAt;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updatedAt;
}

