package campus.ai.rag.service.vo.admin.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "AiCallTrendPointVO", description = "AI 调用趋势点")
public class AiCallTrendPointVO {

    @ApiModelProperty(value = "时间分组标签", example = "2026-04-20")
    private String bucket;

    @ApiModelProperty(value = "调用次数", example = "120")
    private Long callCount;

    @ApiModelProperty(value = "成功率（百分比）", example = "98.50")
    private Double successRate;

    @ApiModelProperty(value = "平均响应时长（毫秒）", example = "820")
    private Long avgElapsedMs;
}
