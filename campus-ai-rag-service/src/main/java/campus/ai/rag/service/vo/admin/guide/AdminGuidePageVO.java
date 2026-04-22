package campus.ai.rag.service.vo.admin.guide;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel(value = "AdminGuidePageVO", description = "后台办事指南分页")
public class AdminGuidePageVO {

    @ApiModelProperty(value = "页码", example = "1")
    private Long pageNum;

    @ApiModelProperty(value = "每页条数", example = "20")
    private Long pageSize;

    @ApiModelProperty(value = "总条数", example = "45")
    private Long total;

    @ApiModelProperty(value = "列表")
    private List<AdminGuideVO> list;
}
