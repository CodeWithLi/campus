package campus.ai.rag.service.vo.wx;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("日程提醒")
public class WxScheduleReminderVO {

    @ApiModelProperty("提醒ID")
    private Long id;

    @ApiModelProperty("标题")
    private String title;

    @ApiModelProperty("提醒类型")
    private String type;

    @ApiModelProperty("提醒类型中文")
    private String typeText;

    @ApiModelProperty("提醒时间（yyyy-MM-dd HH:mm:ss）")
    private String remindAt;

    @ApiModelProperty("是否已触发通知")
    private Boolean notified;
}
