package campus.ai.rag.service.vo.wx;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("发帖 AI 生成结果")
public class WxPostAiGenerateVO {

    @ApiModelProperty("草稿 ID，发布未改稿时回传以跳过文本合规")
    private String draftId;

    @ApiModelProperty("标题")
    private String title;

    @ApiModelProperty("正文")
    private String content;

    @ApiModelProperty("实际使用的模型编码")
    private String modelCode;

    @ApiModelProperty("计费场景 POST")
    private String scene;
}
