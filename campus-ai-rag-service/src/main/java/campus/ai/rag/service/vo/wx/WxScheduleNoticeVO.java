package campus.ai.rag.service.vo.wx;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("提醒通知")
public class WxScheduleNoticeVO {

    @ApiModelProperty("通知ID")
    private Long id;

    @ApiModelProperty("提醒ID")
    private Long reminderId;

    @ApiModelProperty("标题")
    private String title;

    @ApiModelProperty("提醒类型")
    private String type;

    @ApiModelProperty("提醒类型中文")
    private String typeText;

    @ApiModelProperty("通知内容")
    private String content;

    @ApiModelProperty("是否已读")
    private Boolean read;

    @ApiModelProperty("通知时间（yyyy-MM-dd HH:mm:ss）")
    private String createdAt;
}
