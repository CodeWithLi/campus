package campus.ai.rag.service.vo.admin.kb;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "AdminWxKbDocumentPreviewVO", description = "后台用户文档预览返回")
public class AdminWxKbDocumentPreviewVO {

    @ApiModelProperty(value = "文档ID", example = "1001")
    private Long id;

    @ApiModelProperty(value = "用户ID", example = "12")
    private Long userId;

    @ApiModelProperty(value = "文档标题")
    private String title;

    @ApiModelProperty(value = "原始文件名")
    private String originalFileName;

    @ApiModelProperty(value = "文件后缀", example = "pdf")
    private String fileExt;

    @ApiModelProperty(value = "处理状态")
    private String processStatus;

    @ApiModelProperty(value = "预览文本")
    private String previewContent;
}

