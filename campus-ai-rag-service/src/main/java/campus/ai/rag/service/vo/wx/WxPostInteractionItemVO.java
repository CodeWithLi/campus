package campus.ai.rag.service.vo.wx;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("我的互动项")
public class WxPostInteractionItemVO {

    @ApiModelProperty("记录ID")
    private Long id;

    @ApiModelProperty("互动类型：LIKE/COMMENT")
    private String interactionType;

    @ApiModelProperty("互动用户ID")
    private Long actorUserId;

    @ApiModelProperty("互动用户昵称")
    private String actorNickname;

    @ApiModelProperty("帖子ID")
    private Long postId;

    @ApiModelProperty("帖子标题")
    private String postTitle;

    @ApiModelProperty("评论ID")
    private Long commentId;

    @ApiModelProperty("互动内容摘要")
    private String contentSnapshot;

    @ApiModelProperty("创建时间")
    private String createdAt;
}
