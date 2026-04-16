package campus.ai.rag.service.dto.wx;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "WxKbUploadDTO", description = "微信端个人知识库上传参数")
public class WxKbUploadDTO {

    @ApiModelProperty(value = "文档标题", required = true, example = "我的课程笔记")
    private String title;

    @ApiModelProperty(value = "备注")
    private String remark;
}

