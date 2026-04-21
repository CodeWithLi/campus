package campus.ai.rag.service.vo.wx;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel("简历生成记录项")
public class WxResumeRecordItemVO {

    @ApiModelProperty("记录ID")
    private Long id;

    @ApiModelProperty("模板类型")
    private String templateType;

    @ApiModelProperty("岗位方向")
    private String jobDirection;

    @ApiModelProperty("简历文本摘要")
    private String resumeTextPreview;

    @ApiModelProperty("生成链接")
    private List<WxResumeGenerateVO.LinkVO> links;

    @ApiModelProperty("创建时间")
    private String createdAt;
}
