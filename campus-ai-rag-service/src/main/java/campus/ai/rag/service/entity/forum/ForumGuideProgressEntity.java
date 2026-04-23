package campus.ai.rag.service.entity.forum;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("forum_guide_progress")
public class ForumGuideProgressEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long guideId;

    private Long stepId;

    private Integer done;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

