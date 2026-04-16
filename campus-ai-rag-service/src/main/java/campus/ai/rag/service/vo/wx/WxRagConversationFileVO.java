package campus.ai.rag.service.vo.wx;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "WxRagConversationFileVO", description = "RAG会话关联文件返回参数")
public class WxRagConversationFileVO {

    @ApiModelProperty(value = "文档ID", example = "12")
    private Long documentId;

    @ApiModelProperty(value = "文档标题", example = "我的课程笔记")
    private String title;

    @ApiModelProperty(value = "原始文件名", example = "lecture1.pdf")
    private String originalFileName;

    @ApiModelProperty(value = "文件后缀", example = "pdf")
    private String fileExt;

    @ApiModelProperty(value = "处理状态：PENDING/PROCESSING/SUCCESS/FAILED")
    private String processStatus;

    @ApiModelProperty(value = "切片数量", example = "12")
    private Integer chunkCount;
}

