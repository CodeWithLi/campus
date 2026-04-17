package campus.ai.rag.service.controller.wx.speech;

import campus.ai.rag.service.common.Result;
import campus.ai.rag.service.service.wx.speech.WxSpeechService;
import campus.ai.rag.service.vo.wx.WxSpeechTranscribeVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/wx/speech")
@Api(tags = "微信端语音接口")
@Tag(name = "微信端语音")
public class WxSpeechController {

    private final WxSpeechService wxSpeechService;

    public WxSpeechController(WxSpeechService wxSpeechService) {
        this.wxSpeechService = wxSpeechService;
    }

    @PostMapping("/transcribe")
    @ApiOperation(value = "语音转写", notes = "上传音频并返回识别文本")
    @Operation(summary = "语音转写")
    public Result<WxSpeechTranscribeVO> transcribe(@RequestParam("file") MultipartFile file,
                                                    @RequestParam(value = "scene", required = false) String scene,
                                                    @RequestParam(value = "durationMs", required = false) Long durationMs) {
        return Result.success(wxSpeechService.transcribe(file, scene, durationMs));
    }
}
