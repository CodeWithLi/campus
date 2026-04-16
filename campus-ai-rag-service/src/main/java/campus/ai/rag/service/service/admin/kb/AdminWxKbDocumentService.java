package campus.ai.rag.service.service.admin.kb;

import campus.ai.rag.service.vo.admin.kb.AdminWxKbDocumentPageVO;
import campus.ai.rag.service.vo.admin.kb.AdminWxKbDocumentPreviewVO;
import campus.ai.rag.service.vo.admin.kb.AdminWxKbDocumentVO;

public interface AdminWxKbDocumentService {

    AdminWxKbDocumentPageVO listPage(Long pageNum, Long pageSize, Long userId);

    AdminWxKbDocumentVO getById(Long id);

    AdminWxKbDocumentPreviewVO preview(Long id);
}

