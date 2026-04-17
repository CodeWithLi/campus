package campus.ai.rag.service.dto.wx;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "WxResumeGenerateDTO", description = "微信端调用星火简历生成请求参数")
public class WxResumeGenerateDTO {

    @ApiModelProperty(value = "简历模板类型：CAMPUS/EXPERIENCED/INTERN", example = "CAMPUS")
    private String templateType;

    @ApiModelProperty(value = "岗位方向（可选）", example = "Java后端开发")
    private String jobDirection;

    @ApiModelProperty(value = "简历结构化信息文本（可由用户输入或上一步AI对话产出）", required = true)
    private String resumeText;
}

