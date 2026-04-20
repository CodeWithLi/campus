package campus.ai.rag.service.service.wx.auth;

import campus.ai.rag.service.dto.wx.WxEmailCodeDTO;
import campus.ai.rag.service.dto.wx.WxLoginDTO;
import campus.ai.rag.service.dto.wx.WxPasswordUpdateDTO;
import campus.ai.rag.service.dto.wx.WxRegisterDTO;
import campus.ai.rag.service.vo.admin.AdminLoginVO;
import campus.ai.rag.service.vo.wx.WxEmailCodeVO;
import campus.ai.rag.service.vo.wx.WxPowerPointLogPageVO;
import campus.ai.rag.service.vo.wx.WxProfileVO;
import org.springframework.web.multipart.MultipartFile;

public interface WxAuthService {

    WxEmailCodeVO sendEmailCode(WxEmailCodeDTO dto);

    AdminLoginVO register(WxRegisterDTO dto);

    AdminLoginVO login(WxLoginDTO dto);

    WxEmailCodeVO sendPasswordUpdateCode();

    Boolean updatePassword(WxPasswordUpdateDTO dto);

    WxProfileVO getProfile();

    WxPowerPointLogPageVO listPowerPointLogs(Long pageNum, Long pageSize);

    String uploadAvatar(MultipartFile file);
}

