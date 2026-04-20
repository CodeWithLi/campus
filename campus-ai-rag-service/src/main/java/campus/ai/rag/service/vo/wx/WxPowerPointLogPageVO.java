package campus.ai.rag.service.vo.wx;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel("微信端算力点扣费明细分页")
public class WxPowerPointLogPageVO {

    @ApiModelProperty(value = "当前页码", example = "1")
    private Long pageNum;

    @ApiModelProperty(value = "每页条数", example = "20")
    private Long pageSize;

    @ApiModelProperty(value = "总条数", example = "56")
    private Long total;

    @ApiModelProperty(value = "明细列表")
    private List<WxPowerPointLogVO> list;
}

