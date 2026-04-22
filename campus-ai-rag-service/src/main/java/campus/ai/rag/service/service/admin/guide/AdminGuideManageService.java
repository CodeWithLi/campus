package campus.ai.rag.service.service.admin.guide;

import campus.ai.rag.service.dto.admin.guide.AdminGuideSaveDTO;
import campus.ai.rag.service.vo.admin.guide.AdminGuidePageVO;
import campus.ai.rag.service.vo.admin.guide.AdminGuideVO;
import org.springframework.web.multipart.MultipartFile;

public interface AdminGuideManageService {

    AdminGuidePageVO listPage(Long pageNum, Long pageSize, String keyword, String status, String category);

    AdminGuideVO detail(Long guideId);

    AdminGuideVO create(AdminGuideSaveDTO dto);

    AdminGuideVO update(Long guideId, AdminGuideSaveDTO dto);

    void publish(Long guideId);

    void offline(Long guideId);

    void delete(Long guideId);

    String uploadCover(MultipartFile file);
}
