package campus.ai.rag.service.vo.wx;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel(value = "WxKbDocumentVO", description = "微信端个人知识库文档返回")
public class WxKbDocumentVO {

    @ApiModelProperty(value = "文档ID", example = "1")
    private Long id;

    @ApiModelProperty(value = "标题")
    private String title;

    @ApiModelProperty(value = "原文件名")
    private String originalFileName;

    @ApiModelProperty(value = "扩展名")
    private String fileExt;

    @ApiModelProperty(value = "处理状态", example = "SUCCESS")
    private String processStatus;

    @ApiModelProperty(value = "失败原因")
    private String failReason;

    @ApiModelProperty(value = "切片数量", example = "12")
    private Integer chunkCount;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createdAt;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updatedAt;
}

