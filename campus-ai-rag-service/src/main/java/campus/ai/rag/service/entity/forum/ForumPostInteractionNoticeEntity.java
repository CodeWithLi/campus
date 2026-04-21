package campus.ai.rag.service.entity.forum;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("forum_post_interaction_notice")
public class ForumPostInteractionNoticeEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long receiverUserId;

    private Long actorUserId;

    private Long postId;

    private Long commentId;

    private String interactionType;

    private String contentSnapshot;

    private Integer isRead;

    private LocalDateTime createdAt;
}
