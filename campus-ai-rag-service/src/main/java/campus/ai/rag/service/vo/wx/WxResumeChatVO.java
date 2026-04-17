package campus.ai.rag.service.vo.wx;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "WxResumeChatVO", description = "微信端简历AI对话返回参数")
public class WxResumeChatVO {

    @ApiModelProperty(value = "本次请求实际使用的模型编码", example = "qwen_plus")
    private String modelCode;

    @ApiModelProperty(value = "会话ID", example = "rsm_7f3a1c4b0a6f4d2e")
    private String conversationId;

    @ApiModelProperty(value = "会话标题（首轮后生成）", example = "Java后端实习简历")
    private String conversationTitle;

    @ApiModelProperty(value = "AI回复内容")
    private String answer;
}

