package campus.ai.rag.service.entity.rbac;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对应表：rbac_permission
 */
@Data
@TableName("rbac_permission")
public class RbacPermissionEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String permCode;

    private String permName;

    private String clientScope;

    private String apiPattern;

    private String httpMethod;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

