package campus.ai.rag.service.service.wx.speech;

import campus.ai.rag.service.vo.wx.WxSpeechTranscribeVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.function.Consumer;

public interface WxSpeechService {

    WxSpeechTranscribeVO transcribe(MultipartFile file, String scene, Long durationMs);

    WxSpeechTranscribeVO transcribeNew(MultipartFile file, String scene, Long durationMs);

    WxSpeechTranscribeVO transcribeNewStream(MultipartFile file,
                                             String scene,
                                             Long durationMs,
                                             Consumer<String> partialConsumer);
}
