package campus.ai.rag.service.entity.forum;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("forum_post_report")
public class ForumPostReportEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long postId;

    private Long reporterUserId;

    private String reason;

    /**
     * PENDING / RESOLVED_VALID / RESOLVED_INVALID
     */
    private String status;

    private String handleRemark;

    private String handledBy;

    private LocalDateTime handledAt;

    private LocalDateTime createdAt;
}
