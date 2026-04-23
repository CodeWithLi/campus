package campus.ai.rag.service.entity.forum;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("forum_material_template")
public class ForumMaterialTemplateEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String templateName;

    private String itemsJson;

    private Integer enabled;

    private Integer sortNo;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

