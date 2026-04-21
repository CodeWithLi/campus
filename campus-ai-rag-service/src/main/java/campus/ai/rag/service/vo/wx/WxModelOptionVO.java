package campus.ai.rag.service.vo.wx;

import lombok.Data;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@Data
@ApiModel(value = "WxModelOptionVO", description = "微信端可见模型选项")
public class WxModelOptionVO {

    @ApiModelProperty(value = "模型编码（唯一）", example = "qwen_plus")
    private String modelCode;
    @ApiModelProperty(value = "模型展示名称", example = "通义千问Plus")
    private String modelName;
    @ApiModelProperty(value = "模型提供商", example = "qwen")
    private String provider;
    @ApiModelProperty(value = "调用方式：1=springai，2=http", example = "1")
    private Integer invokeType;
    @ApiModelProperty(value = "是否支持图片对话：1=支持，0=不支持", example = "0")
    private Integer supportsImageChat;
    @ApiModelProperty(value = "排序号（越小越靠前）", example = "10")
    private Integer sortNo;
}

