package campus.ai.rag.service.vo.wx;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("帖子收藏结果")
public class WxPostFavoriteVO {

    @ApiModelProperty("帖子ID")
    private Long postId;

    @ApiModelProperty("是否已收藏")
    private Boolean favorited;
}
