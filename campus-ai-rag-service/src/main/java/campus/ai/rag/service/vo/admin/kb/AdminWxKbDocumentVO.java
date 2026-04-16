package campus.ai.rag.service.vo.admin.kb;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel(value = "AdminWxKbDocumentVO", description = "后台用户个人知识库文档返回参数")
public class AdminWxKbDocumentVO {

    @ApiModelProperty(value = "文档ID", example = "1001")
    private Long id;

    @ApiModelProperty(value = "用户ID", example = "12")
    private Long userId;

    @ApiModelProperty(value = "文档标题")
    private String title;

    @ApiModelProperty(value = "原始文件名")
    private String originalFileName;

    @ApiModelProperty(value = "文件后缀", example = "pdf")
    private String fileExt;

    @ApiModelProperty(value = "文件MIME类型")
    private String contentType;

    @ApiModelProperty(value = "文件大小（字节）")
    private Long fileSize;

    @ApiModelProperty(value = "处理状态：PENDING/PROCESSING/SUCCESS/FAILED")
    private String processStatus;

    @ApiModelProperty(value = "失败原因")
    private String failReason;

    @ApiModelProperty(value = "切片数量")
    private Integer chunkCount;

    @ApiModelProperty(value = "处理完成时间")
    private LocalDateTime processedAt;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createdAt;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updatedAt;
}

