package campus.ai.rag.service.dto.admin.post;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("后台处理帖子举报参数")
public class AdminPostReportHandleDTO {

    @ApiModelProperty(value = "处理结果：RESOLVED_VALID / RESOLVED_INVALID", required = true)
    private String status;

    @ApiModelProperty(value = "处理备注", example = "已核实并删除违规帖子")
    private String handleRemark;

    @ApiModelProperty(value = "是否同时删除帖子（仅在判定违规时可选）", example = "true")
    private Boolean deletePost;
}
