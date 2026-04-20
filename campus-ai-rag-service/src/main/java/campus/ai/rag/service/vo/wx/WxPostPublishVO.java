package campus.ai.rag.service.vo.wx;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("发帖发布结果")
public class WxPostPublishVO {

    @ApiModelProperty("帖子 ID")
    private Long postId;

    @ApiModelProperty("是否跳过了文本合规（仅 AI 草稿完全一致时为 true）")
    private Boolean textComplianceSkipped;
}
