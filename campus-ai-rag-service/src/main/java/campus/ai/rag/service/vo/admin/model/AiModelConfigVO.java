package campus.ai.rag.service.vo.admin.model;

import lombok.Data;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.time.LocalDateTime;

@Data
@ApiModel(value = "AiModelConfigVO", description = "后台模型配置返回参数")
public class AiModelConfigVO {

    @ApiModelProperty(value = "主键ID", example = "1")
    private Long id;
    @ApiModelProperty(value = "模型编码", example = "qwen_plus")
    private String modelCode;
    @ApiModelProperty(value = "模型展示名称", example = "通义千问Plus")
    private String modelName;
    @ApiModelProperty(value = "调用方式：1=springai，2=xf_http(讯飞星火)，3=generic_http(通用HTTP)", example = "1")
    private Integer invokeType;
    @ApiModelProperty(value = "模型提供商", example = "qwen")
    private String provider;
    @ApiModelProperty(value = "调用地址或baseUrl", example = "https://dashscope.aliyuncs.com/compatible-mode")
    private String apiBaseUrl;
    @ApiModelProperty(value = "调用密钥（已脱敏）", example = "sk-1****9abc")
    private String apiKey;
    @ApiModelProperty(value = "远程模型名称", example = "qwen-plus")
    private String modelNameRemote;
    @ApiModelProperty(value = "温度参数", example = "0.7")
    private Double temperature;
    @ApiModelProperty(value = "最大输出Token数", example = "4096")
    private Integer maxTokens;

    @ApiModelProperty(value = "通用HTTP请求体类型：messages/prompt/input（invokeType=3时使用）", example = "messages")
    private String genericReqType;

    @ApiModelProperty(value = "通用HTTP请求体字段名（invokeType=3时使用）", example = "messages")
    private String genericReqField;

    @ApiModelProperty(value = "通用HTTP响应提取路径（invokeType=3时使用）", example = "reply")
    private String genericRespPath;

    @ApiModelProperty(value = "通用HTTP额外请求头JSON（invokeType=3时使用）")
    private String genericHeadersJson;
    @ApiModelProperty(value = "启用状态：1=启用，0=禁用", example = "1")
    private Integer enabled;
    @ApiModelProperty(value = "排序号", example = "10")
    private Integer sortNo;
    @ApiModelProperty(value = "备注", example = "Spring AI方式调用")
    private String remark;
    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createdAt;
    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updatedAt;
}

