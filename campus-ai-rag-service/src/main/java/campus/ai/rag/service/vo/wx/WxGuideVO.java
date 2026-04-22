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

    @ApiModelProperty(value = "步骤列表")
    private List<WxGuideStepVO> steps;
}
