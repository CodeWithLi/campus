package campus.ai.rag.service.dto.wx;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel("通知智能体批量创建提醒")
public class WxNoticeAgentCreateDTO {

    @ApiModelProperty("解析记录ID（可选）")
    private Long recordId;

    @ApiModelProperty("提醒项")
    private List<ItemDTO> items;

    @Data
    public static class ItemDTO {
        @ApiModelProperty("提醒标题")
        private String title;
        @ApiModelProperty("提醒类型：exam/ddl/activity")
        private String type;
        @ApiModelProperty("提醒时间（毫秒时间戳）")
        private Long remindAt;
    }
}
