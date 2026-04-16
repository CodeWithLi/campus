package campus.ai.rag.service.vo.wx;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel(value = "WxChatConversationDetailVO", description = "微信端-会话详情返回参数")
public class WxChatConversationDetailVO {

    @ApiModelProperty(value = "会话标题", example = "安全邮箱绑定")
    private String conversationTitle;

    @ApiModelProperty(value = "消息列表（按时间顺序，user/assistant）")
    private List<WxChatConversationMessageVO> messages;

    @Data
    @ApiModel(value = "WxChatConversationMessageVO", description = "会话消息")
    public static class WxChatConversationMessageVO {
        @ApiModelProperty(value = "角色：user/assistant", example = "user")
        private String role;

        @ApiModelProperty(value = "消息内容", example = "你好")
        private String content;
    }
}

