package campus.ai.rag.service.vo.wx;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("微信端知识库分类")
public class WxKbCategoryVO {

    @ApiModelProperty("分类ID")
    private Long id;

    @ApiModelProperty("分类编码")
    private String categoryCode;

    @ApiModelProperty("分类名称")
    private String categoryName;
}
