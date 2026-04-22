package campus.ai.rag.service.dto.admin.guide;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "AdminGuideStepSaveDTO", description = "后台保存指南步骤参数")
public class AdminGuideStepSaveDTO {

    @ApiModelProperty(value = "步骤序号", example = "1")
    private Integer stepNo;

    @ApiModelProperty(value = "步骤标题", required = true, example = "进入请假系统")
    private String title;

    @ApiModelProperty(value = "步骤说明", example = "登录后点击请假申请菜单")
    private String content;

    @ApiModelProperty(value = "动作类型：NONE/LINK/REMINDER/ASK_AI", example = "LINK")
    private String actionType;

    @ApiModelProperty(value = "动作参数JSON", example = "{\"url\":\"https://example.com\"}")
    private String actionPayload;

    @ApiModelProperty(value = "材料清单JSON", example = "[\"学生证\",\"请假证明\"]")
    private String requiredMaterials;

    @ApiModelProperty(value = "步骤启用状态：1启用，0停用", example = "1")
    private Integer status;
}
