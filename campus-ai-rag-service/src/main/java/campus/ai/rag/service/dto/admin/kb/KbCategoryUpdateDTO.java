package campus.ai.rag.service.dto.admin.kb;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "KbCategoryUpdateDTO", description = "后台更新知识库分类请求参数")
public class KbCategoryUpdateDTO {

    @ApiModelProperty(value = "分类名称", example = "学生处")
    private String categoryName;

    @ApiModelProperty(value = "是否启用：1=启用，0=禁用", example = "1")
    private Integer enabled;

    @ApiModelProperty(value = "排序号（越小越靠前）", example = "20")
    private Integer sortNo;

    @ApiModelProperty(value = "备注", example = "学生事务相关文档")
    private String remark;
}

