package campus.ai.rag.service.vo.admin.guide;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel(value = "AdminMaterialTemplateVO", description = "后台材料模板")
public class AdminMaterialTemplateVO {

    @ApiModelProperty(value = "模板ID", example = "1")
    private Long id;

    @ApiModelProperty(value = "模板名称")
    private String templateName;

    @ApiModelProperty(value = "材料项JSON数组")
    private String itemsJson;

    @ApiModelProperty(value = "是否启用：1启用，0禁用")
    private Integer enabled;

    @ApiModelProperty(value = "排序值")
    private Integer sortNo;

    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createdAt;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updatedAt;
}

