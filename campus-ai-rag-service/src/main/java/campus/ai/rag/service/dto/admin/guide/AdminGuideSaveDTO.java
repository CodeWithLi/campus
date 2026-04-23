package campus.ai.rag.service.dto.admin.guide;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel(value = "AdminGuideSaveDTO", description = "后台保存办事指南参数")
public class AdminGuideSaveDTO {

    @ApiModelProperty(value = "指南标题", required = true, example = "请假流程")
    private String title;

    @ApiModelProperty(value = "分类", example = "学业")
    private String category;

    @ApiModelProperty(value = "标签", example = "高频")
    private String tag;

    @ApiModelProperty(value = "简介", example = "学生请假申请标准流程")
    private String summary;

    @ApiModelProperty(value = "封面图URL", example = "https://example.com/cover.png")
    private String coverUrl;

    @ApiModelProperty(value = "关联RAG知识库分类ID列表JSON（如 [1,2]）")
    private String ragCategoryIdsJson;

    @ApiModelProperty(value = "排序值", example = "10")
    private Integer sortNo;

    @ApiModelProperty(value = "步骤列表")
    private List<AdminGuideStepSaveDTO> steps;
}
