package campus.ai.rag.service.vo.admin.order;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel(value = "AdminRechargeOrderPageVO", description = "后台充值订单分页数据")
public class AdminRechargeOrderPageVO {

    @ApiModelProperty(value = "页码", example = "1")
    private Long pageNum;

    @ApiModelProperty(value = "每页条数", example = "20")
    private Long pageSize;

    @ApiModelProperty(value = "总条数", example = "128")
    private Long total;

    @ApiModelProperty(value = "订单列表")
    private List<AdminRechargeOrderVO> list;
}
