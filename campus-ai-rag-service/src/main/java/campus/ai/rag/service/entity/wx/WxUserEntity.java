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

    private String username;

    private String email;

    private String nickname;

    private String avatarUrl;

    /**
     * 用户状态：1=正常，0=禁用
     */
    private Integer status;

    /**
     * 注册来源：PASSWORD/WX
     */
    private String registerSource;

    /**
     * 剩余算力点
     */
    private Integer powerPoints;

    private LocalDateTime lastLoginAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

