package campus.ai.rag.service.entity.prompt;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对应表：ai_system_prompt
 */
@Data
@TableName("ai_system_prompt")
public class AiSystemPromptEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String promptCode;

    private String promptName;

    /**
     * 适用场景：CHAT/RAG/RESUME/BOTH
     */
    private String scene;

    /**
     * 系统提示词正文（system role 内容）
     */
    private String content;

    /**
     * 启用状态：1=启用，0=禁用
     */
    private Integer enabled;

    /**
     * 优先级（越小越优先）
     */
    private Integer priority;

    /**
     * 是否默认：1=默认，0=否
     */
    private Integer isDefault;

    /**
     * 版本号（每次修改递增）
     */
    private Integer version;

    private String remark;

    private Long createdBy;

    private Long updatedBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

