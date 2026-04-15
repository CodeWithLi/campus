package campus.ai.rag.service.dto.admin.kb;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "KbCategoryCreateDTO", description = "后台新增知识库分类请求参数")
public class KbCategoryCreateDTO {

    @ApiModelProperty(value = "分类编码（唯一）", required = true, example = "ACADEMIC")
    private String categoryCode;

    @ApiModelProperty(value = "分类名称", required = true, example = "教务")
    private String categoryName;

    @ApiModelProperty(value = "是否启用：1=启用，0=禁用", example = "1")
    private Integer enabled;

    @ApiModelProperty(value = "排序号（越小越靠前）", example = "10")
    private Integer sortNo;

    @ApiModelProperty(value = "备注", example = "教务通知和办事指南")
    private String remark;
}

