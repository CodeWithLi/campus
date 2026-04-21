package campus.ai.rag.service.vo.admin.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "AiModelTestChatVO", description = "后台模型连通测试结果")
public class AiModelTestChatVO {

    @ApiModelProperty(value = "模型编码", example = "qwen plus")
    private String modelCode;

    @ApiModelProperty(value = "测试问题", example = "你好")
    private String question;

    @ApiModelProperty(value = "模型回复")
    private String answer;

    @ApiModelProperty(value = "耗时（毫秒）", example = "680")
    private Long elapsedMs;
}

