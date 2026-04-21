package campus.ai.rag.service.vo.admin.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ApiModel(value = "AiTokenConsumptionPointVO", description = "Token 与积分消耗趋势点")
public class AiTokenConsumptionPointVO {

    @ApiModelProperty(value = "时间分组标签", example = "2026-04-20")
    private String bucket;

    @ApiModelProperty(value = "输入 token 总量", example = "2200")
    private Long promptTokens;

    @ApiModelProperty(value = "输出 token 总量", example = "3400")
    private Long completionTokens;

    @ApiModelProperty(value = "总 token", example = "5600")
    private Long totalTokens;

    @ApiModelProperty(value = "扣减积分", example = "82.50")
    private BigDecimal consumedPoints;
}
