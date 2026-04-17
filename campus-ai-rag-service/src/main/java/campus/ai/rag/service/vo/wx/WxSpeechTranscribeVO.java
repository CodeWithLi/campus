package campus.ai.rag.service.vo.wx;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "WxSpeechTranscribeVO", description = "微信端语音转写返回")
public class WxSpeechTranscribeVO {

    @ApiModelProperty(value = "识别文本", example = "请帮我总结一下这段内容")
    private String text;

    @ApiModelProperty(value = "场景", example = "chat")
    private String scene;

    @ApiModelProperty(value = "音频时长（毫秒）", example = "3200")
    private Long durationMs;

    @ApiModelProperty(value = "转写供应商", example = "xf-fast-asr")
    private String provider;
}
