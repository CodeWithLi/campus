package campus.ai.rag.service.vo.admin.kb;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel(value = "KbRagDebugVO", description = "后台RAG调试返回结果")
public class KbRagDebugVO {

    @ApiModelProperty(value = "使用的模型编码", example = "qwen_plus")
    private String modelCode;

    @ApiModelProperty(value = "用户问题")
    private String question;

    @ApiModelProperty(value = "最终回答")
    private String answer;

    @ApiModelProperty(value = "召回片段列表")
    private List<Hit> hits;

    @ApiModelProperty(value = "拼接后的RAG上下文（可选返回）")
    private String ragContext;

    @Data
    @ApiModel(value = "KbRagDebugVO.Hit", description = "召回片段")
    public static class Hit {
        @ApiModelProperty(value = "文档ID", example = "7")
        private Long documentId;

        @ApiModelProperty(value = "分类ID", example = "1")
        private Long categoryId;

        @ApiModelProperty(value = "文档标题")
        private String documentTitle;

        @ApiModelProperty(value = "切片序号", example = "3")
        private Integer chunkIndex;

        @ApiModelProperty(value = "向量距离（越小越相近）", example = "0.1234")
        private Double distance;

        @ApiModelProperty(value = "切片内容")
        private String content;
    }
}

