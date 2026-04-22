package campus.ai.rag.service.vo.wx;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "WxGuideListItemVO", description = "微信端办事指南列表项")
public class WxGuideListItemVO {

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
}
