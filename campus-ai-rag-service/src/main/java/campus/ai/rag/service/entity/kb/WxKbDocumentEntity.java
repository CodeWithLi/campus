package campus.ai.rag.service.entity.kb;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("wx_kb_document")
public class WxKbDocumentEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long categoryId;
    private String title;
    private String originalFileName;
    private String fileExt;
    private String contentType;
    private Long fileSize;
    private String storagePath;
    private String processStatus;
    private String failReason;
    private Integer retryCount;
    private String embeddingModel;
    private Integer chunkCount;
    private String parsedContent;
    private LocalDateTime processedAt;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

