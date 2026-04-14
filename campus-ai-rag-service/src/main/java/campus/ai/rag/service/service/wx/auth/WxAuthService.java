package campus.ai.rag.service.service.wx.auth;

import campus.ai.rag.service.dto.wx.WxEmailCodeDTO;
import campus.ai.rag.service.dto.wx.WxLoginDTO;
import campus.ai.rag.service.dto.wx.WxRegisterDTO;
import campus.ai.rag.service.vo.admin.AdminLoginVO;
import campus.ai.rag.service.vo.wx.WxEmailCodeVO;

public interface WxAuthService {

    WxEmailCodeVO sendEmailCode(WxEmailCodeDTO dto);

    AdminLoginVO register(WxRegisterDTO dto);

    AdminLoginVO login(WxLoginDTO dto);
}

