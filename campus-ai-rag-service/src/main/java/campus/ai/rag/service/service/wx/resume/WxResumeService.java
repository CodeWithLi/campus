package campus.ai.rag.service.service.wx.resume;

import campus.ai.rag.service.dto.wx.WxResumeChatDTO;
import campus.ai.rag.service.dto.wx.WxResumeGenerateDTO;
import campus.ai.rag.service.vo.wx.WxResumeChatVO;
import campus.ai.rag.service.vo.wx.WxResumeGenerateVO;

public interface WxResumeService {

    WxResumeChatVO chat(WxResumeChatDTO dto);

    WxResumeGenerateVO generate(WxResumeGenerateDTO dto);
}

