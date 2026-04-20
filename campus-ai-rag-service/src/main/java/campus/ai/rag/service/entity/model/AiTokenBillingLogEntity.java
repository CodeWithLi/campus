package campus.ai.rag.service.entity.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 对应表：ai_token_billing_log
 */
@Data
@TableName("ai_token_billing_log")
public class AiTokenBillingLogEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    /**
     * CHAT / RAG / INTERNAL
     */
    private String scene;

    private String modelCode;

    /**
     * 调用方式：1=springai，2=xf_http，3=generic_http
     */
    private Integer invokeType;

    /**
     * 计费类型：TOKEN_API / ESTIMATE_CHAR
     */
    private String billingType;

    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;

    private Integer requestChars;
    private Integer responseChars;

    /**
     * 本次扣减算力点
     */
    private BigDecimal consumedPoints;

    /**
     * 状态：1成功 0失败
     */
    private Integer status;

    private String errorMessage;

    /**
     * 扩展字段 JSON（原始 usage 等）
     */
    private String extJson;

    private LocalDateTime createdAt;
}

