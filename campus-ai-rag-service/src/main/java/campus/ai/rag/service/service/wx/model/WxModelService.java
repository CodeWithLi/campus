package campus.ai.rag.service.service.wx.model;

import campus.ai.rag.service.vo.wx.WxModelOptionVO;

import java.util.List;

public interface WxModelService {

    List<WxModelOptionVO> listEnabledModels();
}

