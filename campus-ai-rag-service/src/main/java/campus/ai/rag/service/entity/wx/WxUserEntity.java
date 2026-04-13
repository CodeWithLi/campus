package campus.ai.rag.service.entity.wx;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对应表：wx_user
 */
@Data
@TableName("wx_user")
public class WxUserEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String openId;

    private String unionId;

    private String username;

    private String password;

    private String nickname;

    private String avatarUrl;

    private String phone;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

