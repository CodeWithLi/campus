package campus.ai.rag.service.vo.wx;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ApiModel("微信端支付宝下单返回")
public class WxAlipayCreateOrderVO {

    @ApiModelProperty(value = "商户订单号")
    private String orderNo;

    @ApiModelProperty(value = "支付金额（元）")
    private BigDecimal amountYuan;

    @ApiModelProperty(value = "充值算力点")
    private BigDecimal points;

    @ApiModelProperty(value = "支付宝支付URL（WAP GET方式）")
    private String payUrl;
}

