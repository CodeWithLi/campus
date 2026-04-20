package campus.ai.rag.service.vo.wx;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel("帖子分页结果")
public class WxPostPageVO {

    @ApiModelProperty("总数")
    private Long total;

    @ApiModelProperty("列表")
    private List<WxPostListItemVO> list;
}
