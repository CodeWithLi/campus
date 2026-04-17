package campus.ai.rag.service.dto.admin.prompt;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "AiSystemPromptCreateDTO", description = "后台新增系统提示词请求参数")
public class AiSystemPromptCreateDTO {

    @ApiModelProperty(value = "提示词编码（唯一，用于程序引用）", required = true, example = "SYS_CHAT_DEFAULT")
    private String promptCode;

    @ApiModelProperty(value = "提示词名称（后台展示）", required = true, example = "系统提示词-对话默认")
    private String promptName;

    @ApiModelProperty(value = "适用场景：CHAT/RAG/RESUME/BOTH", required = true, example = "CHAT")
    private String scene;

    @ApiModelProperty(value = "提示词正文（system role）", required = true, example = "你是校园智能助理...")
    private String content;

    @ApiModelProperty(value = "启用状态：1=启用，0=禁用", example = "1")
    private Integer enabled;

    @ApiModelProperty(value = "优先级（越小越优先）", example = "10")
    private Integer priority;

    @ApiModelProperty(value = "是否默认：1=默认，0=否", example = "1")
    private Integer isDefault;

    @ApiModelProperty(value = "备注", example = "对话默认系统提示词")
    private String remark;
}

