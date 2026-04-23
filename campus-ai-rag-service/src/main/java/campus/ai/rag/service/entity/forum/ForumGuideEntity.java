package campus.ai.rag.service.entity.forum;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("forum_guide")
public class ForumGuideEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String title;

    private String category;

    private String tag;

    private String summary;

    private String coverUrl;

    private String ragCategoryIdsJson;

    private String status;

    private Integer sortNo;

    private Long viewCount;

    private Integer deleted;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
