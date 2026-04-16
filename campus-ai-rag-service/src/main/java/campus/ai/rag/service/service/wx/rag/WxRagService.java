package campus.ai.rag.service.service.wx.rag;

import campus.ai.rag.service.dto.wx.WxRagDTO;
import campus.ai.rag.service.vo.wx.WxRagVO;

public interface WxRagService {

    WxRagVO rag(WxRagDTO dto);
}

