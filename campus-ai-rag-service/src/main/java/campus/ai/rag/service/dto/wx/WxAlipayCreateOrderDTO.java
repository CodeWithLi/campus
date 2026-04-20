package campus.ai.rag.service.dto.wx;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ApiModel("微信端支付宝下单参数")
public class WxAlipayCreateOrderDTO {

    @ApiModelProperty(value = "充值金额（元），仅支持 1/30/50/100", required = true, example = "30")
    private BigDecimal amountYuan;
}

