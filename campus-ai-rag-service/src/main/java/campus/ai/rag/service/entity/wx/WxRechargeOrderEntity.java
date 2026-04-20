package campus.ai.rag.service.entity.wx;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("wx_recharge_order")
public class WxRechargeOrderEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long userId;

    private String channel;

    private BigDecimal amountYuan;

    private BigDecimal points;

    private String subject;

    private String status;

    private String tradeNo;

    private String buyerLogonId;

    private LocalDateTime paidAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

