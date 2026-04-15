package campus.ai.rag.service.dto.admin.kb;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "KbDocumentUpdateStatusDTO", description = "后台更新文档处理状态请求参数")
public class KbDocumentUpdateStatusDTO {

    @ApiModelProperty(value = "处理状态：PENDING/PROCESSING/SUCCESS/FAILED", required = true, example = "FAILED")
    private String processStatus;

    @ApiModelProperty(value = "失败原因（状态为FAILED时建议传）", example = "文档解析失败：不支持的编码")
    private String failReason;
}

