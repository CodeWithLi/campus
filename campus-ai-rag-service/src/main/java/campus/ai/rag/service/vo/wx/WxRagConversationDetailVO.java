package campus.ai.rag.service.vo.wx;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel(value = "WxRagConversationDetailVO", description = "微信端-RAG会话详情返回参数")
public class WxRagConversationDetailVO {

    @ApiModelProperty(value = "会话标题", example = "安全邮箱绑定")
    private String conversationTitle;

    @ApiModelProperty(value = "消息列表（按时间顺序，user/assistant）")
    private List<WxChatConversationDetailVO.WxChatConversationMessageVO> messages;

    @ApiModelProperty(value = "该对话关联的用户上传文件列表")
    private List<WxRagConversationFileVO> files;
}

