package campus.ai.rag.service.mapper.wx;

import campus.ai.rag.service.entity.wx.WxUserEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface WxUserMapper extends BaseMapper<WxUserEntity> {

    @Select("SELECT id, username, email, nickname, avatar_url, status, register_source, power_points, "
            + "last_login_at, created_at, updated_at FROM wx_user WHERE username = #{username} LIMIT 1")
    WxUserEntity findByUsername(@Param("username") String username);

    @Select("SELECT id, username, email, nickname, avatar_url, status, register_source, power_points, "
            + "last_login_at, created_at, updated_at FROM wx_user WHERE email = #{email} LIMIT 1")
    WxUserEntity findByEmail(@Param("email") String email);

    /**
     * 原子扣减用户算力点：power_points = power_points - #{points}
     * 当 power_points 不足时不会更新（WHERE power_points >= #{points}）。
     *
     * @return 受影响行数（成功=1，失败=0）
     */
    @Update("UPDATE wx_user SET power_points = power_points - #{points} "
            + "WHERE id = #{userId} AND power_points >= #{points}")
    int deductPowerPoints(@Param("userId") Long userId, @Param("points") int points);
}

