package campus.ai.rag.service.vo.wx;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("帖子举报结果")
public class WxPostReportVO {

    @ApiModelProperty("举报记录ID")
    private Long reportId;

    @ApiModelProperty("帖子ID")
    private Long postId;

    @ApiModelProperty("状态")
    private String status;
}
