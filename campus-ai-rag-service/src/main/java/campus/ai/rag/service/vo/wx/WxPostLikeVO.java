package campus.ai.rag.service.vo.wx;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("帖子点赞结果")
public class WxPostLikeVO {

    @ApiModelProperty("帖子ID")
    private Long postId;

    @ApiModelProperty("当前用户是否已点赞")
    private Boolean liked;

    @ApiModelProperty("点赞总数")
    private Integer likeCount;
}
