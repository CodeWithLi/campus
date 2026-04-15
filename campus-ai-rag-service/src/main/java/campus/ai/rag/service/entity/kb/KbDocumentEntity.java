package campus.ai.rag.service.entity.kb;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对应表：kb_document
 */
@Data
@TableName("kb_document")
public class KbDocumentEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long categoryId;

    private String title;

    private String originalFileName;

    private String fileExt;

    private String contentType;

    private Long fileSize;

    private String storagePath;

    /**
     * 处理状态：PENDING/PROCESSING/SUCCESS/FAILED
     */
    private String processStatus;

    /**
     * 失败原因
     */
    private String failReason;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 向量模型：固定 text-embedding-v4（1024维）
     */
    private String embeddingModel;

    /**
     * 切片数量
     */
    private Integer chunkCount;

    /**
     * 解析后的纯文本内容（用于后续切片和向量化）
     */
    private String parsedContent;

    /**
     * 图片理解结果文本（用于前端回显与向量增强）
     */
    private String imageUnderstandText;

    private LocalDateTime processedAt;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

