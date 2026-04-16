package campus.ai.rag.service.service.admin.model;

import campus.ai.rag.service.vo.admin.model.AiTokenBillingLogPageVO;

public interface AdminTokenBillingLogService {

    AiTokenBillingLogPageVO listPage(Long pageNum,
                                      Long pageSize,
                                      Long userId,
                                      String scene,
                                      String modelCode,
                                      Integer status,
                                      String startTime,
                                      String endTime);
}

