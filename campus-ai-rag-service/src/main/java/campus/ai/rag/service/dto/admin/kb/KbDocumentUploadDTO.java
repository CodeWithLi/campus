package campus.ai.rag.service.dto.admin.kb;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "KbDocumentUploadDTO", description = "后台上传知识库文档参数")
public class KbDocumentUploadDTO {

    @ApiModelProperty(value = "分类ID", required = true, example = "1")
    private Long categoryId;

    @ApiModelProperty(value = "文档标题", required = true, example = "奖助学金申请细则（2026版）")
    private String title;

    @ApiModelProperty(value = "备注", example = "每年更新一次")
    private String remark;
}

