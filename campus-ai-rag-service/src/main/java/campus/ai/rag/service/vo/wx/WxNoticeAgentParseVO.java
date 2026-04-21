package campus.ai.rag.service.vo.wx;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel("通知智能体解析结果")
public class WxNoticeAgentParseVO {

    @ApiModelProperty("解析记录ID")
    private Long recordId;

    @ApiModelProperty("OCR路由：IMAGE_OCR/PDF_PAGE_OCR/PDF_API_OCR")
    private String route;

    @ApiModelProperty("OCR原始文本")
    private String rawText;

    @ApiModelProperty("结构化摘要")
    private String summary;

    @ApiModelProperty("提醒候选列表")
    private List<ItemVO> items;

    @Data
    public static class ItemVO {
        @ApiModelProperty("提醒标题")
        private String title;
        @ApiModelProperty("提醒类型：exam/ddl/activity")
        private String type;
        @ApiModelProperty("提醒时间（毫秒时间戳）")
        private Long remindAt;
        @ApiModelProperty("来源说明")
        private String source;
    }
}
