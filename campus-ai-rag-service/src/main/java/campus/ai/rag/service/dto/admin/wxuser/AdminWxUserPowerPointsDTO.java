package campus.ai.rag.service.dto.admin.wxuser;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "AdminWxUserPowerPointsDTO", description = "后台调整用户算力点请求参数")
public class AdminWxUserPowerPointsDTO {

    @ApiModelProperty(value = "新的算力点（必须>=0）", required = true, example = "300")
    private Integer powerPoints;
}

