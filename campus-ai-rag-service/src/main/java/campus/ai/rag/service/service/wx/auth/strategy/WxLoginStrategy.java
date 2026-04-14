package campus.ai.rag.service.service.wx.auth.strategy;

import campus.ai.rag.service.dto.wx.WxLoginDTO;
import campus.ai.rag.service.vo.admin.AdminLoginVO;

public interface WxLoginStrategy {

    String supportType();

    AdminLoginVO login(WxLoginDTO dto);
}

