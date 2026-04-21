package campus.ai.rag.service.vo.admin.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "AiModelUsageShareVO", description = "模型调用占比")
public class AiModelUsageShareVO {

    @ApiModelProperty(value = "模型编码", example = "qwen plus")
    private String modelCode;

    @ApiModelProperty(value = "调用次数", example = "320")
    private Long callCount;

    @ApiModelProperty(value = "调用占比（百分比）", example = "45.30")
    private Double usageRate;
}
