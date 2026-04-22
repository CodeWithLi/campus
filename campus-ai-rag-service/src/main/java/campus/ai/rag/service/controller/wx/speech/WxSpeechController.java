package campus.ai.rag.service.controller.wx.speech;

import campus.ai.rag.service.common.Result;
import campus.ai.rag.service.service.wx.speech.WxSpeechService;
import campus.ai.rag.service.vo.wx.WxSpeechTranscribeVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/wx/speech")
@Api(tags = "微信端语音接口")
@Tag(name = "微信端语音")
public class WxSpeechController {

    private final WxSpeechService wxSpeechService;

    public WxSpeechController(WxSpeechService wxSpeechService) {
        this.wxSpeechService = wxSpeechService;
    }

    @PostMapping("/transcribe/old")
    @ApiOperation(value = "语音转写", notes = "上传音频并返回识别文本")
    @Operation(summary = "语音转写")
    public Result<WxSpeechTranscribeVO> transcribe(@RequestParam("file") MultipartFile file,
                                                    @RequestParam(value = "scene", required = false) String scene,
                                                    @RequestParam(value = "durationMs", required = false) Long durationMs) {
        return Result.success(wxSpeechService.transcribe(file, scene, durationMs));
    }

    @PostMapping("/transcribe")
    @ApiOperation(value = "语音转写（新）", notes = "上传音频并返回识别文本")
    @Operation(summary = "语音转写（新）")
    public Result<WxSpeechTranscribeVO> transcribeNew(@RequestParam("file") MultipartFile file,
                                                       @RequestParam(value = "scene", required = false) String scene,
                                                       @RequestParam(value = "durationMs", required = false) Long durationMs) {
        return Result.success(wxSpeechService.transcribeNew(file, scene, durationMs));
    }

    @PostMapping(value = "/transcribe/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ApiOperation(value = "语音转写（流式）", notes = "上传音频并实时返回识别文本")
    @Operation(summary = "语音转写（流式）")
    public SseEmitter transcribeStream(@RequestParam("file") MultipartFile file,
                                       @RequestParam(value = "scene", required = false) String scene,
                                       @RequestParam(value = "durationMs", required = false) Long durationMs) {
        SseEmitter emitter = new SseEmitter(120000L);
        CompletableFuture.runAsync(() -> {
            try {
                WxSpeechTranscribeVO finalVo = wxSpeechService.transcribeNewStream(file, scene, durationMs, partial -> {
                    try {
                        emitter.send(SseEmitter.event().name("partial").data(partial == null ? "" : partial));
                    } catch (IOException ignore) {
                    }
                });
                emitter.send(SseEmitter.event().name("final").data(finalVo));
                emitter.send(SseEmitter.event().name("done").data("ok"));
                emitter.complete();
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                } catch (IOException ignore) {
                }
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }
}
