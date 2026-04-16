package campus.ai.rag.service.vo.wx;

import lombok.Data;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@Data
@ApiModel(value = "WxChatVO", description = "微信端单轮对话返回参数")
public class WxChatVO {

    /**
     * 本次请求实际路由到的模型编码。
     */
    @ApiModelProperty(value = "本次请求实际使用的模型编码", example = "qwen_plus")
    private String modelCode;

    @ApiModelProperty(value = "会话ID", example = "c_7f3a1c4b0a6f4d2e")
    private String conversationId;

    @ApiModelProperty(value = "会话标题（首轮后生成）", example = "安全邮箱绑定")
    private String conversationTitle;

    /**
     * AI 回复内容。
     */
    @ApiModelProperty(value = "AI回复内容", example = "你好，我是你的校园AI助手。")
    private String answer;
}

