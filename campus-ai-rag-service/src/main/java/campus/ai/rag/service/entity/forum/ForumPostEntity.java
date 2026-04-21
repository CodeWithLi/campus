package campus.ai.rag.service.entity.forum;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("forum_post")
public class ForumPostEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title;

    private String content;

    /**
     * 图片 URL JSON 数组字符串。
     */
    private String imageUrls;

    /**
     * 是否匿名发布：1=匿名，0=非匿名
     */
    private Integer isAnonymous;

    /**
     * 1=未改动的 AI 正文，未调用文本合规；0=已走人工审核或人工编辑后发布
     */
    private Integer textComplianceSkipped;

    private LocalDateTime createdAt;
}
