package campus.ai.rag.service.entity.rbac;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对应表：rbac_role
 */
@Data
@TableName("rbac_role")
public class RbacRoleEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String roleCode;

    private String roleName;

    private Integer builtIn;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

