package campus.ai.rag.service.vo.admin.post;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel("后台帖子举报项")
public class AdminPostReportVO {

    @ApiModelProperty("举报ID")
    private Long id;

    @ApiModelProperty("帖子ID")
    private Long postId;

    @ApiModelProperty("帖子标题")
    private String postTitle;

    @ApiModelProperty("帖子发布用户ID")
    private Long postUserId;

    @ApiModelProperty("帖子发布用户昵称")
    private String postUserNickname;

    @ApiModelProperty("举报用户ID")
    private Long reporterUserId;

    @ApiModelProperty("举报用户昵称")
    private String reporterNickname;

    @ApiModelProperty("举报理由")
    private String reason;

    @ApiModelProperty("处理状态")
    private String status;

    @ApiModelProperty("处理备注")
    private String handleRemark;

    @ApiModelProperty("处理人")
    private String handledBy;

    @ApiModelProperty("处理时间")
    private LocalDateTime handledAt;

    @ApiModelProperty("举报时间")
    private LocalDateTime createdAt;
}
