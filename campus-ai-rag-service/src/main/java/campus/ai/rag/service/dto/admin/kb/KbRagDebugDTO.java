package campus.ai.rag.service.dto.admin.kb;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel(value = "KbRagDebugDTO", description = "后台RAG调试请求参数")
public class KbRagDebugDTO {

    @ApiModelProperty(value = "用户问题", required = true, example = "如何绑定安全邮箱？")
    private String question;

    @ApiModelProperty(value = "模型编码（可选，不传则使用默认模型）", example = "qwen_plus")
    private String modelCode;

    @ApiModelProperty(value = "分类ID列表（可选，不传则全库检索）", example = "[1,2]")
    private List<Long> categoryIds;

    @ApiModelProperty(value = "TopK（可选，默认5）", example = "5")
    private Integer topK;

    @ApiModelProperty(value = "是否返回拼接后的RAG上下文（默认true）", example = "true")
    private Boolean returnRagContext;
}

