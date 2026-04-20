package campus.ai.rag.service.mapper.wx;

import campus.ai.rag.service.entity.wx.WxPowerPointCreditLogEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

@Mapper
public interface WxPowerPointCreditLogMapper extends BaseMapper<WxPowerPointCreditLogEntity> {

    /**
     * 幂等插入：同一 order_no 只会成功插入一次。
     *
     * @return 1=插入成功；0=已存在（不重复加点）
     */
    @Insert("INSERT IGNORE INTO wx_power_point_credit_log (user_id, order_no, channel, points) "
            + "VALUES (#{userId}, #{orderNo}, #{channel}, #{points})")
    int insertIgnore(@Param("userId") Long userId,
                     @Param("orderNo") String orderNo,
                     @Param("channel") String channel,
                     @Param("points") BigDecimal points);
}

