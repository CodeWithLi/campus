package campus.ai.rag.service.vo.wx;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "WxGuideStepVO", description = "微信端办事指南步骤")
public class WxGuideStepVO {

    @ApiModelProperty(value = "步骤ID", example = "100")
    private Long id;

    @ApiModelProperty(value = "步骤序号", example = "1")
    private Integer stepNo;

    @ApiModelProperty(value = "步骤标题")
    private String title;

    @ApiModelProperty(value = "步骤说明")
    private String content;

    @ApiModelProperty(value = "动作类型")
    private String actionType;

    @ApiModelProperty(value = "动作参数JSON")
    private String actionPayload;

    @ApiModelProperty(value = "材料清单JSON")
    private String requiredMaterials;
}
