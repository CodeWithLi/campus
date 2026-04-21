package campus.ai.rag.service.vo.admin.post;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel(value = "AdminPostPageVO", description = "后台帖子分页")
public class AdminPostPageVO {

    @ApiModelProperty(value = "页码", example = "1")
    private Long pageNum;

    @ApiModelProperty(value = "每页条数", example = "20")
    private Long pageSize;

    @ApiModelProperty(value = "总条数", example = "86")
    private Long total;

    @ApiModelProperty(value = "帖子列表")
    private List<AdminPostVO> list;
}
