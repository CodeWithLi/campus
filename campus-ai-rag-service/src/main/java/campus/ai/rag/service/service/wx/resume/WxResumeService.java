package campus.ai.rag.service.service.wx.resume;

import campus.ai.rag.service.dto.wx.WxResumeChatDTO;
import campus.ai.rag.service.dto.wx.WxResumeGenerateDTO;
import campus.ai.rag.service.vo.wx.WxResumeChatVO;
import campus.ai.rag.service.vo.wx.WxResumeGenerateVO;
import campus.ai.rag.service.vo.wx.WxResumeRecordPageVO;
import java.util.function.Consumer;

public interface WxResumeService {

    WxResumeChatVO chat(WxResumeChatDTO dto);

    WxResumeChatVO chatStream(WxResumeChatDTO dto, Consumer<String> onDelta);

    WxResumeGenerateVO generate(WxResumeGenerateDTO dto);

    WxResumeRecordPageVO listGenerateRecords(Integer pageNo, Integer pageSize);
}

