package campus.ai.rag.service.service.wx.chat;

import campus.ai.rag.service.dto.wx.WxChatDTO;
import campus.ai.rag.service.vo.wx.WxChatVO;
import java.util.function.Consumer;

public interface WxChatService {

    WxChatVO chat(WxChatDTO dto);

    WxChatVO chatStream(WxChatDTO dto, Consumer<String> onDelta);
}

