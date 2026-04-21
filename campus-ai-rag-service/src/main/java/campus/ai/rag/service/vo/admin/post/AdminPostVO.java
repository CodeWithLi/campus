package campus.ai.rag.service.vo.admin.post;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@ApiModel(value = "AdminPostVO", description = "后台帖子管理项")
public class AdminPostVO {

    @ApiModelProperty(value = "帖子ID", example = "1")
    private Long id;

    @ApiModelProperty(value = "发布用户ID", example = "10001")
    private Long userId;

    @ApiModelProperty(value = "发布用户名")
    private String username;

    @ApiModelProperty(value = "发布昵称")
    private String nickname;

    @ApiModelProperty(value = "帖子标题")
    private String title;

    @ApiModelProperty(value = "帖子正文")
    private String content;

    @ApiModelProperty(value = "图片 URL 列表")
    private List<String> imageUrls;

    @ApiModelProperty(value = "是否匿名：1=匿名，0=非匿名", example = "0")
    private Integer isAnonymous;

    @ApiModelProperty(value = "是否跳过文本合规：1=是，0=否", example = "0")
    private Integer textComplianceSkipped;

    @ApiModelProperty(value = "点赞数", example = "12")
    private Long likeCount;

    @ApiModelProperty(value = "评论数", example = "8")
    private Long commentCount;

    @ApiModelProperty(value = "发布时间")
    private LocalDateTime createdAt;
}
