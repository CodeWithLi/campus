package campus.ai.rag.service.vo.admin.guide;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel(value = "AdminGuideStepVO", description = "后台办事指南步骤")
public class AdminGuideStepVO {

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

    @ApiModelProperty(value = "启用状态：1启用，0停用")
    private Integer status;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updatedAt;
}
