package campus.ai.rag.service.dto.wx;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("帖子评论创建")
public class WxPostCommentCreateDTO {

    @ApiModelProperty(value = "评论内容", required = true)
    private String content;

    @ApiModelProperty(value = "父评论ID（回复时可传）")
    private Long parentCommentId;

    @ApiModelProperty(value = "回复目标评论ID（显示‘回复谁’）")
    private Long replyToCommentId;
}
