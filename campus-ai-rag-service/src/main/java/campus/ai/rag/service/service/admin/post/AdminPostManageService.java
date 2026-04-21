package campus.ai.rag.service.service.admin.post;

import campus.ai.rag.service.vo.admin.post.AdminPostPageVO;
import campus.ai.rag.service.vo.admin.post.AdminPostVO;

public interface AdminPostManageService {

    AdminPostPageVO listPage(Long pageNum,
                             Long pageSize,
                             String keyword,
                             Long userId,
                             Integer isAnonymous,
                             String startTime,
                             String endTime);

    AdminPostVO detail(Long postId);

    void deletePost(Long postId);
}
