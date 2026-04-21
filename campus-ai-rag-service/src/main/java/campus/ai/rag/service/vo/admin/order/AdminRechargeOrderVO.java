package campus.ai.rag.service.vo.admin.order;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@ApiModel(value = "AdminRechargeOrderVO", description = "后台充值订单信息")
public class AdminRechargeOrderVO {

    @ApiModelProperty(value = "主键ID", example = "1")
    private Long id;

    @ApiModelProperty(value = "订单号", example = "ALI1745236123000ABCD1234")
    private String orderNo;

    @ApiModelProperty(value = "用户ID", example = "10001")
    private Long userId;

    @ApiModelProperty(value = "用户名")
    private String username;

    @ApiModelProperty(value = "昵称")
    private String nickname;

    @ApiModelProperty(value = "支付渠道", example = "ALIPAY_WAP")
    private String channel;

    @ApiModelProperty(value = "订单金额（元）", example = "30.00")
    private BigDecimal amountYuan;

    @ApiModelProperty(value = "充值算力点", example = "3000.00")
    private BigDecimal points;

    @ApiModelProperty(value = "订单标题")
    private String subject;

    @ApiModelProperty(value = "订单状态", example = "PAID")
    private String status;

    @ApiModelProperty(value = "支付宝交易号")
    private String tradeNo;

    @ApiModelProperty(value = "买家账号")
    private String buyerLogonId;

    @ApiModelProperty(value = "支付时间")
    private LocalDateTime paidAt;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createdAt;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updatedAt;
}
