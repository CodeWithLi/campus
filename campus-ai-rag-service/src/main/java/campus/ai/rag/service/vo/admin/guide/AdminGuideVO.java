package campus.ai.rag.service.vo.admin.guide;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@ApiModel(value = "AdminGuideVO", description = "后台办事指南")
public class AdminGuideVO {

    @ApiModelProperty(value = "指南ID", example = "1")
    private Long id;

    @ApiModelProperty(value = "指南标题")
    private String title;

    @ApiModelProperty(value = "分类")
    private String category;

    @ApiModelProperty(value = "标签")
    private String tag;

    @ApiModelProperty(value = "简介")
    private String summary;

    @ApiModelProperty(value = "封面图URL")
    private String coverUrl;

    @ApiModelProperty(value = "状态：DRAFT/PUBLISHED/OFFLINE")
    private String status;

    @ApiModelProperty(value = "排序值")
    private Integer sortNo;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createdAt;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updatedAt;

    @ApiModelProperty(value = "步骤列表")
    private List<AdminGuideStepVO> steps;
}
