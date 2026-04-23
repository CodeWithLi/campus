package campus.ai.rag.service.vo.wx;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel(value = "WxGuideVO", description = "微信端办事指南详情")
public class WxGuideVO {

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

  @ApiModelProperty(value = "关联RAG知识库分类ID列表JSON（如 [1,2]）")
  private String ragCategoryIdsJson;

  @ApiModelProperty(value = "是否已收藏（登录时返回）")
  private Boolean favorite;

  @ApiModelProperty(value = "已完成步骤ID列表（登录时返回）")
  private java.util.List<Long> doneStepIds;

  @ApiModelProperty(value = "完成数量（登录时返回）")
  private Integer doneCount;

  @ApiModelProperty(value = "总步骤数（登录时返回）")
  private Integer totalSteps;

    @ApiModelProperty(value = "步骤列表")
    private List<WxGuideStepVO> steps;
}
