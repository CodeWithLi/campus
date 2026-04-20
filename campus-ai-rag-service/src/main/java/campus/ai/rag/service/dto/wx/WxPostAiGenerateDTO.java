package campus.ai.rag.service.dto.wx;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("微信发帖 AI 生成")
public class WxPostAiGenerateDTO {

    @ApiModelProperty(value = "发帖想法/要点，由模型扩展为标题与正文", required = true)
    private String hint;
}
