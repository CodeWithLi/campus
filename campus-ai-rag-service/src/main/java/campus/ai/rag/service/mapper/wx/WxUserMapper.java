package campus.ai.rag.service.mapper.wx;

import campus.ai.rag.service.entity.wx.WxUserEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface WxUserMapper extends BaseMapper<WxUserEntity> {

    @Select("""
            SELECT id, open_id, union_id, username, password, nickname, avatar_url, phone, status, created_at, updated_at
            FROM wx_user
            WHERE username = #{username}
            LIMIT 1
            """)
    WxUserEntity findByUsername(@Param("username") String username);
}

