package campus.ai.rag.service.vo.wx;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel("提醒通知列表")
public class WxScheduleNoticePageVO {

    @ApiModelProperty("未读数量")
    private Integer unreadCount;

    @ApiModelProperty("通知列表")
    private List<WxScheduleNoticeVO> list;
}
