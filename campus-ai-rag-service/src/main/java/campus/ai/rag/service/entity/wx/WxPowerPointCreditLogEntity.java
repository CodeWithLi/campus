package campus.ai.rag.service.entity.wx;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("wx_power_point_credit_log")
public class WxPowerPointCreditLogEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String orderNo;

    private String channel;

    private BigDecimal points;

    private LocalDateTime createdAt;
}

