package campus.ai.rag.service.dto.wx;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("帖子举报参数")
public class WxPostReportDTO {

    @ApiModelProperty(value = "举报理由", required = true, example = "含有不良信息")
    private String reason;
}
