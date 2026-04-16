package campus.ai.rag.service.vo.wx;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel(value = "WxRagVO", description = "微信端RAG对话返回参数")
public class WxRagVO {

    @ApiModelProperty(value = "本次请求实际使用的模型编码", example = "qwen_plus")
    private String modelCode;

    @ApiModelProperty(value = "本次RAG会话ID（用于拉取历史/文件）", example = "r_7f3a1c4b0a6f4d2e")
    private String conversationId;

    @ApiModelProperty(value = "会话标题（可能为空）", example = "安全邮箱绑定")
    private String conversationTitle;

    @ApiModelProperty(value = "AI回复内容")
    private String answer;

    @ApiModelProperty(value = "召回片段（可用于前端展示来源）")
    private List<Hit> hits;

    @Data
    @ApiModel(value = "WxRagVO.Hit", description = "召回片段")
    public static class Hit {
        @ApiModelProperty(value = "命中来源范围：SYSTEM=系统库，USER=个人库", example = "SYSTEM")
        private String scope;

        @ApiModelProperty(value = "文档ID", example = "7")
        private Long documentId;

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

