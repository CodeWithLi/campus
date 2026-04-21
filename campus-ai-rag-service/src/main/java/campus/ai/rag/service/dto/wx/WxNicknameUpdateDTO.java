package campus.ai.rag.service.dto.wx;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("微信端修改昵称参数")
public class WxNicknameUpdateDTO {

    @ApiModelProperty(value = "新昵称", required = true)
    private String nickname;
}
