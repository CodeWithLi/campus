package campus.ai.rag.service.entity.forum;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("forum_guide_step")
public class ForumGuideStepEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long guideId;

    private Integer stepNo;

    private String title;

    private String content;

    private String actionType;

    private String actionPayload;

    private String requiredMaterials;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
