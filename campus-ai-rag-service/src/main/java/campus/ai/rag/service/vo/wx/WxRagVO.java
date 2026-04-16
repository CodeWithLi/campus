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

