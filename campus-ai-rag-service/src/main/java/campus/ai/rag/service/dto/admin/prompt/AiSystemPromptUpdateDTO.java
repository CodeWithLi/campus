package campus.ai.rag.service.dto.admin.prompt;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "AiSystemPromptUpdateDTO", description = "后台更新系统提示词请求参数")
public class AiSystemPromptUpdateDTO {

    @ApiModelProperty(value = "提示词名称（后台展示）", example = "系统提示词-对话默认")
    private String promptName;

    @ApiModelProperty(value = "适用场景：CHAT/RAG/BOTH", example = "CHAT")
    private String scene;

    @ApiModelProperty(value = "提示词正文（system role）", example = "你是校园智能助理...")
    private String content;

    @ApiModelProperty(value = "启用状态：1=启用，0=禁用", example = "1")
    private Integer enabled;

    @ApiModelProperty(value = "优先级（越小越优先）", example = "10")
    private Integer priority;

    @ApiModelProperty(value = "是否默认：1=默认，0=否", example = "1")
    private Integer isDefault;

    @ApiModelProperty(value = "备注", example = "RAG默认系统提示词")
    private String remark;
}

