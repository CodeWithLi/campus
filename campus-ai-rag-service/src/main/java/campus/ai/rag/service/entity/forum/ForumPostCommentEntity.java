package campus.ai.rag.service.entity.forum;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("forum_post_comment")
public class ForumPostCommentEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long postId;

    private Long userId;

    private String content;

    private Long parentCommentId;

    private Long replyToCommentId;

    private Integer deleted;

    private LocalDateTime createdAt;
}
