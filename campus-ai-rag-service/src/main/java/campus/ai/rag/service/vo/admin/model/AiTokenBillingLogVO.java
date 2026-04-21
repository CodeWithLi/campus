package campus.ai.rag.service.vo.admin.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@ApiModel(value = "AiTokenBillingLogVO", description = "Token 消耗日志返回参数")
public class AiTokenBillingLogVO {

    @ApiModelProperty(value = "主键ID", example = "1")
    private Long id;

    @ApiModelProperty(value = "用户ID", example = "-1")
    private Long userId;

    @ApiModelProperty(value = "场景：CHAT/RAG/INTERNAL", example = "CHAT")
    private String scene;

    @ApiModelProperty(value = "模型编码", example = "qwen_plus")
    private String modelCode;

    @ApiModelProperty(value = "调用方式：1=springai，2=xf_http，3=generic_http", example = "1")
    private Integer invokeType;

    @ApiModelProperty(value = "计费类型：TOKEN_API/ESTIMATE_CHAR", example = "TOKEN_API")
    private String billingType;

    @ApiModelProperty(value = "输入Token", example = "1000")
    private Integer promptTokens;

    @ApiModelProperty(value = "输出Token", example = "1200")
    private Integer completionTokens;

    @ApiModelProperty(value = "总Token", example = "2200")
    private Integer totalTokens;

    @ApiModelProperty(value = "扣减算力点", example = "2")
    private BigDecimal consumedPoints;

    @ApiModelProperty(value = "请求耗时（毫秒）", example = "780")
    private Long elapsedMs;

    @ApiModelProperty(value = "状态：1成功 0失败", example = "1")
    private Integer status;

    @ApiModelProperty(value = "失败原因")
    private String errorMessage;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createdAt;
}

