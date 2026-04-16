package campus.ai.rag.service.vo.admin.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel(value = "AiTokenBillingLogPageVO", description = "Token 消耗日志分页返回参数")
public class AiTokenBillingLogPageVO {

    @ApiModelProperty(value = "当前页码", example = "1")
    private Long pageNum;

    @ApiModelProperty(value = "每页条数", example = "20")
    private Long pageSize;

    @ApiModelProperty(value = "总记录数", example = "120")
    private Long total;

    @ApiModelProperty(value = "列表数据")
    private List<AiTokenBillingLogVO> list;
}

