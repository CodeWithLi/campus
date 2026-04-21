package campus.ai.rag.service.service.admin.post;

import campus.ai.rag.service.dto.admin.post.AdminPostReportHandleDTO;
import campus.ai.rag.service.vo.admin.post.AdminPostReportPageVO;
import campus.ai.rag.service.vo.admin.post.AdminPostReportVO;

public interface AdminPostReportManageService {

    AdminPostReportPageVO listPage(Long pageNum,
                                   Long pageSize,
                                   String status,
                                   Long postId,
                                   Long reporterUserId,
                                   String startTime,
                                   String endTime);

    AdminPostReportVO detail(Long reportId);

    AdminPostReportVO handle(Long reportId, AdminPostReportHandleDTO dto);
}
