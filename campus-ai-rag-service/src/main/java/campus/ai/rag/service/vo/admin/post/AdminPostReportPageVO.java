package campus.ai.rag.service.vo.admin.post;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel("后台帖子举报分页")
public class AdminPostReportPageVO {

    @ApiModelProperty("页码")
    private Long pageNum;

    @ApiModelProperty("每页条数")
    private Long pageSize;

    @ApiModelProperty("总条数")
    private Long total;

    @ApiModelProperty("举报列表")
    private List<AdminPostReportVO> list;
}
