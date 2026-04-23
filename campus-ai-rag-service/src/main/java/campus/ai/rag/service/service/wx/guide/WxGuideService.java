package campus.ai.rag.service.service.wx.guide;

import campus.ai.rag.service.vo.wx.WxGuideListItemVO;
import campus.ai.rag.service.vo.wx.WxGuideVO;

import java.util.List;

public interface WxGuideService {

    List<WxGuideListItemVO> listPublished(String keyword, String category);

    List<String> listPublishedCategories();

    WxGuideVO detail(Long guideId);

    Boolean toggleFavorite(Long guideId);

    void updateStepProgress(Long guideId, Long stepId, boolean done);
}
