package campus.ai.rag.service.service.wx.kb;

import campus.ai.rag.service.dto.wx.WxKbUploadDTO;
import campus.ai.rag.service.vo.wx.WxKbDocumentVO;
import org.springframework.web.multipart.MultipartFile;

public interface WxKbDocumentService {

    WxKbDocumentVO upload(WxKbUploadDTO dto, MultipartFile file);
}

