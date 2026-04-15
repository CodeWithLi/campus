package campus.ai.rag.service.entity.kb;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对应表：kb_category
 */
@Data
@TableName("kb_category")
public class KbCategoryEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String categoryCode;

    private String categoryName;

    private Integer enabled;

    private Integer sortNo;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

