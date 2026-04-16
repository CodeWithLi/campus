package campus.ai.rag.service.vo.admin.kb;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel(value = "AdminWxKbDocumentPageVO", description = "后台用户个人知识库文档分页返回")
public class AdminWxKbDocumentPageVO {

    @ApiModelProperty(value = "当前页码", example = "1")
    private Long pageNum;

    @ApiModelProperty(value = "每页条数", example = "10")
    private Long pageSize;

    @ApiModelProperty(value = "总记录数", example = "120")
    private Long total;

    @ApiModelProperty(value = "列表数据")
    private List<AdminWxKbDocumentVO> list;
}

