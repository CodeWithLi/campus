package campus.ai.rag.service.vo.admin.kb;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel(value = "KbDocumentVO", description = "后台知识库文档返回参数")
public class KbDocumentVO {

    @ApiModelProperty(value = "文档ID", example = "1001")
    private Long id;

    @ApiModelProperty(value = "分类ID", example = "1")
    private Long categoryId;

    @ApiModelProperty(value = "文档标题", example = "奖助学金申请细则（2026版）")
    private String title;

    @ApiModelProperty(value = "原始文件名", example = "奖助学金申请细则.pdf")
    private String originalFileName;

    @ApiModelProperty(value = "文件后缀", example = "pdf")
    private String fileExt;

    @ApiModelProperty(value = "文件MIME类型", example = "application/pdf")
    private String contentType;

    @ApiModelProperty(value = "文件大小（字节）", example = "123456")
    private Long fileSize;

    @ApiModelProperty(value = "存储路径", example = "uploads/kb/2026/04/abc.pdf")
    private String storagePath;

    @ApiModelProperty(value = "处理状态：PENDING/PROCESSING/SUCCESS/FAILED", example = "PENDING")
    private String processStatus;

    @ApiModelProperty(value = "失败原因")
    private String failReason;

    @ApiModelProperty(value = "重试次数", example = "0")
    private Integer retryCount;

    @ApiModelProperty(value = "向量模型", example = "text-embedding-v4")
    private String embeddingModel;

    @ApiModelProperty(value = "切片数量", example = "12")
    private Integer chunkCount;

    @ApiModelProperty(value = "解析后的纯文本内容（调试/联调用）")
    private String parsedContent;

    @ApiModelProperty(value = "图片理解结果文本（调试/联调用）")
    private String imageUnderstandText;

    @ApiModelProperty(value = "解析文本长度", example = "3200")
    private Integer parsedLength;

    @ApiModelProperty(value = "处理完成时间")
    private LocalDateTime processedAt;

    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createdAt;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updatedAt;
}

