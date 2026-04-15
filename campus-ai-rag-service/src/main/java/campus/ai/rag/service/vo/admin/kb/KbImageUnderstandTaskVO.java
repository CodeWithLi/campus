package campus.ai.rag.service.vo.admin.kb;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel(value = "KbImageUnderstandTaskVO", description = "图片理解异步任务状态返回")
public class KbImageUnderstandTaskVO {

    @ApiModelProperty(value = "任务ID")
    private String taskId;

    @ApiModelProperty(value = "文档ID", example = "1")
    private Long documentId;

    @ApiModelProperty(value = "任务类型：PREVIEW/INDEX", example = "PREVIEW")
    private String taskType;

    @ApiModelProperty(value = "任务状态：PENDING/PROCESSING/SUCCESS/FAILED", example = "PROCESSING")
    private String taskStatus;

    @ApiModelProperty(value = "任务提示信息")
    private String message;

    @ApiModelProperty(value = "图片数量（预览任务成功后返回）", example = "3")
    private Integer imageCount;

    @ApiModelProperty(value = "图片理解汇总文本（预览任务成功后返回）")
    private String mergedText;

    @ApiModelProperty(value = "向量化结果文档（入库任务成功后返回）")
    private KbDocumentVO document;

    @ApiModelProperty(value = "提交时间")
    private LocalDateTime submittedAt;

    @ApiModelProperty(value = "开始执行时间")
    private LocalDateTime startedAt;

    @ApiModelProperty(value = "结束时间")
    private LocalDateTime finishedAt;
}

