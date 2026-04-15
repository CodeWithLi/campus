package campus.ai.rag.service.vo.admin.kb;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel(value = "KbDocumentPageVO", description = "后台知识库文档分页返回参数")
public class KbDocumentPageVO {

    @ApiModelProperty(value = "当前页码", example = "1")
    private Long pageNum;

    @ApiModelProperty(value = "每页条数", example = "10")
    private Long pageSize;

    @ApiModelProperty(value = "总记录数", example = "120")
    private Long total;

    @ApiModelProperty(value = "当前页数据")
    private List<KbDocumentVO> list;
}

