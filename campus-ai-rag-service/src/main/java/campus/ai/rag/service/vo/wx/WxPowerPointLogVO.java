package campus.ai.rag.service.vo.wx;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@ApiModel("微信端算力点扣费明细项")
public class WxPowerPointLogVO {

    @ApiModelProperty(value = "扣费场景编码", example = "CHAT")
    private String scene;

    @ApiModelProperty(value = "扣费场景名称", example = "AI对话")
    private String sceneName;

    @ApiModelProperty(value = "本次扣减算力点", example = "2")
    private BigDecimal consumedPoints;

    @ApiModelProperty(value = "扣费时间")
    private LocalDateTime createdAt;
}

