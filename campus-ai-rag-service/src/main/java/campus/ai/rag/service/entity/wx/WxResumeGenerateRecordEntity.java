package campus.ai.rag.service.entity.wx;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对应表：wx_resume_generate_record
 */
@Data
@TableName("wx_resume_generate_record")
public class WxResumeGenerateRecordEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String sid;

    private String templateType;

    private String jobDirection;

    private String resumeText;

    private String linksJson;

    private Integer linkCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
