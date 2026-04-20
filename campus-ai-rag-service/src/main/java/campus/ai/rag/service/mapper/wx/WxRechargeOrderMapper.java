package campus.ai.rag.service.mapper.wx;

import campus.ai.rag.service.entity.wx.WxRechargeOrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface WxRechargeOrderMapper extends BaseMapper<WxRechargeOrderEntity> {

    @Update("UPDATE wx_recharge_order SET status = 'PAID', trade_no = #{tradeNo}, buyer_logon_id = #{buyerLogonId}, paid_at = #{paidAt} "
            + "WHERE order_no = #{orderNo} AND status = 'CREATED'")
    int markPaidIfCreated(@Param("orderNo") String orderNo,
                          @Param("tradeNo") String tradeNo,
                          @Param("buyerLogonId") String buyerLogonId,
                          @Param("paidAt") LocalDateTime paidAt);
}

