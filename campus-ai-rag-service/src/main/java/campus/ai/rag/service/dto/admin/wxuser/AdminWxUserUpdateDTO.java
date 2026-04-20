package campus.ai.rag.service.dto.admin.wxuser;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ApiModel(value = "AdminWxUserUpdateDTO", description = "后台更新微信用户请求参数")
public class AdminWxUserUpdateDTO {

    @ApiModelProperty(value = "用户昵称", example = "校园小助手用户")
    private String nickname;

    @ApiModelProperty(value = "邮箱", example = "test@example.com")
    private String email;

    @ApiModelProperty(value = "用户状态：1=正常，0=冻结", example = "1")
    private Integer status;

    @ApiModelProperty(value = "剩余算力点（必须>=0）", example = "200")
    private BigDecimal powerPoints;
}

