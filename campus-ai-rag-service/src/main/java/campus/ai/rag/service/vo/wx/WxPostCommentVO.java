package campus.ai.rag.service.vo.wx;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel("帖子评论")
public class WxPostCommentVO {

    @ApiModelProperty("评论ID")
    private Long id;

    @ApiModelProperty("帖子ID")
    private Long postId;

    @ApiModelProperty("评论用户ID")
    private Long userId;

    @ApiModelProperty("评论用户昵称")
    private String nickname;

    @ApiModelProperty("评论内容")
    private String content;

    @ApiModelProperty("父评论ID")
    private Long parentCommentId;

    @ApiModelProperty("回复目标评论ID")
    private Long replyToCommentId;

    @ApiModelProperty("回复目标昵称")
    private String replyToNickname;

    @ApiModelProperty("评论时间")
    private String createdAt;

    @ApiModelProperty("子评论")
    private List<WxPostCommentVO> children;
}
