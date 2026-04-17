package campus.ai.rag.service.service.wx.speech;

import campus.ai.rag.service.vo.wx.WxSpeechTranscribeVO;
import org.springframework.web.multipart.MultipartFile;

public interface WxSpeechService {

    WxSpeechTranscribeVO transcribe(MultipartFile file, String scene, Long durationMs);
}
