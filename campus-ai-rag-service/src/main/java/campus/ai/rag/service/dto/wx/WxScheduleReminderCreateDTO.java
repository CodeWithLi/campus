package campus.ai.rag.service.dto.wx;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("日程提醒创建")
public class WxScheduleReminderCreateDTO {

    @ApiModelProperty(value = "提醒标题", required = true)
    private String title;

    @ApiModelProperty(value = "提醒类型：exam/ddl/activity", required = true)
    private String type;

    @ApiModelProperty(value = "提醒时间时间戳（毫秒）", required = true)
    private Long remindAt;
}
