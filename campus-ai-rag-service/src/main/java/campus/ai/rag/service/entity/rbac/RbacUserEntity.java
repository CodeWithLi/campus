package campus.ai.rag.service.entity.rbac;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对应表：rbac_user
 */
@Data
@TableName("rbac_user")
public class RbacUserEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private String nickname;

    private Integer status;

    private String superAdminMarker;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

