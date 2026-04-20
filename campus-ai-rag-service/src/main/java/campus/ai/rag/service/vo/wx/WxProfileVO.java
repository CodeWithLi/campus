package campus.ai.rag.service.vo.wx;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ApiModel("微信端个人资料")
public class WxProfileVO {

    @ApiModelProperty("用户ID")
    private Long userId;

    @ApiModelProperty("用户名")
    private String username;

    @ApiModelProperty("邮箱")
    private String email;

    @ApiModelProperty("昵称")
    private String nickname;

    @ApiModelProperty("头像URL（/upload/**）")
    private String avatarUrl;

    @ApiModelProperty(value = "剩余算力点（两位小数）", example = "100.00")
    private BigDecimal powerPoints;
}
