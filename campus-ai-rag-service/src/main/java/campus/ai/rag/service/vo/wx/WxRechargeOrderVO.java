package campus.ai.rag.service.vo.wx;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@ApiModel("微信端充值订单")
public class WxRechargeOrderVO {

    @ApiModelProperty("订单号")
    private String orderNo;

    @ApiModelProperty("支付渠道")
    private String channel;

    @ApiModelProperty("充值金额（元）")
    private BigDecimal amountYuan;

    @ApiModelProperty("到账算力点")
    private BigDecimal points;

    @ApiModelProperty("订单状态")
    private String status;

    @ApiModelProperty("支付时间")
    private LocalDateTime paidAt;

    @ApiModelProperty("创建时间")
    private LocalDateTime createdAt;
}

