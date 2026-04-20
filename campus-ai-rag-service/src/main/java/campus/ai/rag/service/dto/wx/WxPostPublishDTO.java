package campus.ai.rag.service.dto.wx;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel("微信发帖发布")
public class WxPostPublishDTO {

    @ApiModelProperty(value = "标题", required = true)
    private String title;

    @ApiModelProperty(value = "正文，最长 1000 字", required = true)
    private String content;

    /**
     * 来自 /wx/post/ai-generate 的草稿 ID；若用户编辑过正文/标题，请勿传此字段以触发文本合规。
     */
    @ApiModelProperty(value = "AI 草稿 ID（与生成结果未改动时传入可跳过文本合规）")
    private String draftId;

    @ApiModelProperty(value = "帖子图片 URL 列表，最多 5 张")
    private List<String> imageUrls;
}
