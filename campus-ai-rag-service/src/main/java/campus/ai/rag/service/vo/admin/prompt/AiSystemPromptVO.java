package campus.ai.rag.service.vo.admin.prompt;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel(value = "AiSystemPromptVO", description = "后台系统提示词返回参数")
public class AiSystemPromptVO {

    @ApiModelProperty(value = "主键ID", example = "1")
    private Long id;

    @ApiModelProperty(value = "提示词编码", example = "SYS_CHAT_DEFAULT")
    private String promptCode;

    @ApiModelProperty(value = "提示词名称", example = "系统提示词-对话默认")
    private String promptName;

    @ApiModelProperty(value = "适用场景：CHAT/RAG/BOTH", example = "CHAT")
    private String scene;

    @ApiModelProperty(value = "提示词正文（system role）")
    private String content;

    @ApiModelProperty(value = "启用状态：1=启用，0=禁用", example = "1")
    private Integer enabled;

    @ApiModelProperty(value = "优先级（越小越优先）", example = "10")
    private Integer priority;

    @ApiModelProperty(value = "是否默认：1=默认，0=否", example = "1")
    private Integer isDefault;

    @ApiModelProperty(value = "版本号", example = "1")
    private Integer version;

    @ApiModelProperty(value = "备注", example = "对话默认系统提示词")
    private String remark;

    @ApiModelProperty(value = "创建人ID", example = "1")
    private Long createdBy;

    @ApiModelProperty(value = "更新人ID", example = "1")
    private Long updatedBy;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createdAt;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updatedAt;
}

