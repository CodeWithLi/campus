package campus.ai.rag.service.vo.wx;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel(value = "WxResumeGenerateVO", description = "微信端星火简历生成返回参数")
public class WxResumeGenerateVO {

    @ApiModelProperty(value = "星火会话sid", example = "ase000704fa@dx16ade44e4d87a1c802")
    private String sid;

    @ApiModelProperty(value = "生成结果链接")
    private List<LinkVO> links;

    @Data
    public static class LinkVO {
        @ApiModelProperty(value = "简历图片链接")
        private String imgUrl;

        @ApiModelProperty(value = "简历Word链接")
        private String wordUrl;
    }
}

