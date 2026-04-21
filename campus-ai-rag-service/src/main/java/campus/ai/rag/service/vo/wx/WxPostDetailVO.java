package campus.ai.rag.service.vo.wx;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel("帖子详情")
public class WxPostDetailVO {

    @ApiModelProperty("帖子ID")
    private Long id;

    @ApiModelProperty("用户ID")
    private Long userId;

    @ApiModelProperty("用户昵称")
    private String nickname;

    @ApiModelProperty("标题")
    private String title;

    @ApiModelProperty("正文")
    private String content;

    @ApiModelProperty("图片URL列表")
    private List<String> imageUrls;

    @ApiModelProperty("点赞数")
    private Integer likeCount;

    @ApiModelProperty("评论数")
    private Integer commentCount;

    @ApiModelProperty("当前用户是否已点赞")
    private Boolean likedByMe;

    @ApiModelProperty("当前用户是否已收藏")
    private Boolean favoritedByMe;

    @ApiModelProperty("当前用户是否已举报")
    private Boolean reportedByMe;

    @ApiModelProperty("是否匿名发布")
    private Boolean isAnonymous;

    @ApiModelProperty("创建时间")
    private String createdAt;
}
