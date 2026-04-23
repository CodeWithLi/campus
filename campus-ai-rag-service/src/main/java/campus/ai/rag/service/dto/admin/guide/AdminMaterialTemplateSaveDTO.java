package campus.ai.rag.service.dto.admin.guide;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "AdminMaterialTemplateSaveDTO", description = "后台保存材料模板参数")
public class AdminMaterialTemplateSaveDTO {

    @ApiModelProperty(value = "模板名称", required = true, example = "请假材料模板")
    private String templateName;

    @ApiModelProperty(value = "材料项JSON数组", required = true, example = "[\"学生证\",\"请假证明\"]")
    private String itemsJson;

    @ApiModelProperty(value = "是否启用：1启用，0禁用", example = "1")
    private Integer enabled;

    @ApiModelProperty(value = "排序值", example = "100")
    private Integer sortNo;

    @ApiModelProperty(value = "备注", example = "通用请假场景")
    private String remark;
}

