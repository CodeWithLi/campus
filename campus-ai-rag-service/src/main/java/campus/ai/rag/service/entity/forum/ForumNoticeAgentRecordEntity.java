package campus.ai.rag.service.entity.forum;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("forum_notice_agent_record")
public class ForumNoticeAgentRecordEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String fileName;

    private String fileExt;

    private String route;

    private String rawText;

    private String summary;

    private String itemsJson;

    private Integer itemCount;

    /**
     * 逻辑删除标记：0-未删除，1-已删除
     */
    private Integer deleted;

    private LocalDateTime createdAt;
}
