package campus.ai.rag.service.vo.wx;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel("发帖图片上传结果")
public class WxPostUploadImageVO {

    @ApiModelProperty("可访问图片 URL 列表")
    private List<String> imageUrls;
}
