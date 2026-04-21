package campus.ai.rag.service.dto.wx;

import lombok.Data;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@Data
@ApiModel(value = "WxChatDTO", description = "微信端单轮对话请求参数")
public class WxChatDTO {

    /**
     * 模型编码（ai_model_config.model_code，唯一）。
     * 为空时由后端使用默认模型。
     */
    @ApiModelProperty(value = "模型编码（ai_model_config.model_code，唯一）", example = "qwen_plus")
    private String modelCode;

    @ApiModelProperty(value = "会话ID（不传则自动创建新会话）", example = "c_7f3a1c4b0a6f4d2e")
    private String conversationId;

    /**
     * 用户输入内容。
     */
    @ApiModelProperty(value = "用户输入内容", required = true, example = "你好，请介绍一下你自己")
    private String message;

    @ApiModelProperty(value = "图片base64（不含data:image前缀）", example = "iVBORw0KGgoAAA...")
    private String imageBase64;

    @ApiModelProperty(value = "图片MIME类型", example = "image/jpeg")
    private String imageMimeType;
}

