package campus.ai.rag.service.dto.admin.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "AiModelTestChatDTO", description = "后台模型连通测试请求参数")
public class AiModelTestChatDTO {

    @ApiModelProperty(value = "模型编码", required = true, example = "qwen plus")
    private String modelCode;

    @ApiModelProperty(value = "测试问题", example = "你好")
    private String message;
}

