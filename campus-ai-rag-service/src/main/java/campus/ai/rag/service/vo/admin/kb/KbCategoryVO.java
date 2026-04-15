package campus.ai.rag.service.vo.admin.kb;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel(value = "KbCategoryVO", description = "后台知识库分类返回参数")
public class KbCategoryVO {

    @ApiModelProperty(value = "分类ID", example = "1")
    private Long id;

    @ApiModelProperty(value = "分类编码", example = "ACADEMIC")
    private String categoryCode;

    @ApiModelProperty(value = "分类名称", example = "教务")
    private String categoryName;

    @ApiModelProperty(value = "是否启用：1=启用，0=禁用", example = "1")
    private Integer enabled;

    @ApiModelProperty(value = "排序号", example = "10")
    private Integer sortNo;

    @ApiModelProperty(value = "备注", example = "教务通知和办事指南")
    private String remark;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createdAt;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updatedAt;
}

