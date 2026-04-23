package campus.ai.rag.service.dto.wx;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "WxGuideProgressUpdateDTO", description = "更新指南步骤进度")
public class WxGuideProgressUpdateDTO {

    @ApiModelProperty(value = "是否完成：true=完成，false=取消完成", required = true, example = "true")
    private Boolean done;
}

