package campus.ai.rag.service.entity.forum;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("forum_schedule_notice")
public class ForumScheduleNoticeEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long reminderId;

    private String title;

    private String remindType;

    private String content;

    private Integer isRead;

    private LocalDateTime createdAt;
}
