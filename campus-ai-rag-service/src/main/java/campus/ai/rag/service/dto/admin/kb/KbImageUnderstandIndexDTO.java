package campus.ai.rag.service.dto.admin.kb;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "KbImageUnderstandIndexDTO", description = "图片理解后一键向量化请求参数")
public class KbImageUnderstandIndexDTO {

    @ApiModelProperty(value = "前端确认后的图片理解文本（可选，传入则优先使用）")
    private String imageUnderstandText;
}

