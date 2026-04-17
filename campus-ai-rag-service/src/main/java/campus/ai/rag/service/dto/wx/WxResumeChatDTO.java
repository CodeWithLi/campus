package campus.ai.rag.service.dto.wx;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "WxResumeChatDTO", description = "微信端简历AI对话请求参数")
public class WxResumeChatDTO {

    @ApiModelProperty(value = "模型编码（可选，不传使用默认）", example = "qwen_plus")
    private String modelCode;

    @ApiModelProperty(value = "会话ID（不传则自动创建）", example = "rsm_7f3a1c4b0a6f4d2e")
    private String conversationId;

    @ApiModelProperty(value = "用户输入", required = true, example = "帮我生成一份Java后端实习简历，需要突出项目经验")
    private String message;
}

