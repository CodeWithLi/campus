package campus.ai.rag.service.vo.wx;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel("简历生成记录分页")
public class WxResumeRecordPageVO {

    @ApiModelProperty("总数")
    private Long total;

    @ApiModelProperty("列表")
    private List<WxResumeRecordItemVO> list;
}
