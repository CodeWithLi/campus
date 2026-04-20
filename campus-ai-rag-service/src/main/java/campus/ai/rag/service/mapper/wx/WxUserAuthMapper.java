package campus.ai.rag.service.mapper.wx;

import campus.ai.rag.service.entity.wx.WxUserAuthEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface WxUserAuthMapper extends BaseMapper<WxUserAuthEntity> {

    @Select("SELECT id, user_id, auth_type, auth_key, auth_secret, provider, enabled, allow_login, created_at, updated_at "
            + "FROM wx_user_auth WHERE auth_type = #{authType} AND auth_key = #{authKey} LIMIT 1")
    WxUserAuthEntity findByAuthTypeAndAuthKey(@Param("authType") String authType, @Param("authKey") String authKey);

    @Select("SELECT id, user_id, auth_type, auth_key, auth_secret, provider, enabled, allow_login, created_at, updated_at "
            + "FROM wx_user_auth WHERE user_id = #{userId} AND auth_type = #{authType} AND enabled = 1 LIMIT 1")
    WxUserAuthEntity findByUserIdAndAuthType(@Param("userId") Long userId, @Param("authType") String authType);
}

