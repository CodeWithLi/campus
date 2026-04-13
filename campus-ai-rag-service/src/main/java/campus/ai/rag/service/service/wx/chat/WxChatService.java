package campus.ai.rag.service.service.wx.chat;

import campus.ai.rag.service.dto.wx.WxChatDTO;
import campus.ai.rag.service.vo.wx.WxChatVO;

public interface WxChatService {

    WxChatVO chat(WxChatDTO dto);
}

