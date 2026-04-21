package campus.ai.rag.service.vo.wx;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("通知智能体解析记录")
public class WxNoticeAgentRecordVO {

    @ApiModelProperty("记录ID")
    private Long id;

    @ApiModelProperty("文件名")
    private String fileName;

    @ApiModelProperty("路由")
    private String route;

    @ApiModelProperty("摘要")
    private String summary;

    @ApiModelProperty("OCR原文")
    private String rawText;

    @ApiModelProperty("提醒项JSON")
    private String itemsJson;

    @ApiModelProperty("提醒项数量")
    private Integer itemCount;

    @ApiModelProperty("创建时间")
    private String createdAt;
}
