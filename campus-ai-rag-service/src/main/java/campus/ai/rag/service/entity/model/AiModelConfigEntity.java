package campus.ai.rag.service.entity.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对应表：ai_model_config
 */
@Data
@TableName("ai_model_config")
public class AiModelConfigEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String modelCode;

    private String modelName;

    /**
     * 调用方式：1=springai，2=xf_http(讯飞星火)，3=generic_http(通用HTTP)
     */
    private Integer invokeType;

    /**
     * 提供商：qwen/xunfei/deepseek...
     */
    private String provider;

    private String apiBaseUrl;

    private String apiKey;

    private String modelNameRemote;

    private Double temperature;

    private Integer maxTokens;

    /**
     * 通用HTTP请求体类型：messages/prompt/input
     */
    private String genericReqType;

    /**
     * 通用HTTP请求体字段名（例如 messages/prompt/input 的实际字段名）
     */
    private String genericReqField;

    /**
     * 通用HTTP响应提取路径（支持 a.b[0].c），为空则返回原始响应文本
     */
    private String genericRespPath;

    /**
     * 通用HTTP额外请求头 JSON（对象），例如 {"Authorization":"Bearer xxx"}
     */
    private String genericHeadersJson;

    private Integer enabled;

    /**
     * 是否支持图片对话：1=支持，0=不支持
     */
    private Integer supportsImageChat;

    private Integer sortNo;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

