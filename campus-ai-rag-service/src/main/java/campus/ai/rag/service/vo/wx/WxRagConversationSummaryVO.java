package campus.ai.rag.service.vo.wx;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "WxRagConversationSummaryVO", description = "微信端-RAG会话列表项返回参数")
public class WxRagConversationSummaryVO {

    @ApiModelProperty(value = "会话ID", example = "r_7f3a1c4b0a6f4d2e")
    private String conversationId;

    @ApiModelProperty(value = "会话标题（可能为空）", example = "安全邮箱绑定")
    private String conversationTitle;

    @ApiModelProperty(value = "更新时间戳（ms，可能为空）", example = "1710000000000")
    private Long updatedAt;
}

