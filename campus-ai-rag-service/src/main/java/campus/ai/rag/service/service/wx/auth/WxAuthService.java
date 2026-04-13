package campus.ai.rag.service.service.wx.auth;

import campus.ai.rag.service.dto.wx.WxLoginDTO;
import campus.ai.rag.service.vo.admin.AdminLoginVO;

public interface WxAuthService {

    AdminLoginVO login(WxLoginDTO dto);
}

