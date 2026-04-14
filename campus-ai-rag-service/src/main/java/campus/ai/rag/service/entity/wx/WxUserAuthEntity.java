package campus.ai.rag.service.entity.wx;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对应表：wx_user_auth
 */
@Data
@TableName("wx_user_auth")
public class WxUserAuthEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    /**
     * 认证类型：PASSWORD/WX_OPENID
     */
    private String authType;

    /**
     * 认证标识：PASSWORD=用户名；WX_OPENID=openid
     */
    private String authKey;

    /**
     * 认证密钥：PASSWORD=BCrypt密文；WX_OPENID可为空
     */
    private String authSecret;

    /**
     * 认证提供方：WECHAT 等
     */
    private String provider;

    /**
     * 凭据状态：1=启用，0=禁用
     */
    private Integer enabled;

    /**
     * 是否允许登录：1=允许，0=禁止
     */
    private Integer allowLogin;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

