package campus.ai.rag.service.service.admin.model;

import campus.ai.rag.service.vo.admin.model.AiTokenBillingLogPageVO;
import campus.ai.rag.service.vo.admin.model.AiCallTrendPointVO;
import campus.ai.rag.service.vo.admin.model.AiTokenConsumptionPointVO;
import campus.ai.rag.service.vo.admin.model.AiModelUsageShareVO;

import java.util.List;

public interface AdminTokenBillingLogService {

    AiTokenBillingLogPageVO listPage(Long pageNum,
                                      Long pageSize,
                                      Long userId,
                                      String scene,
                                      String modelCode,
                                      Integer status,
                                      String startTime,
                                      String endTime);

    List<AiCallTrendPointVO> callTrend(String granularity, String startTime, String endTime);

    List<AiTokenConsumptionPointVO> tokenConsumption(String granularity, String startTime, String endTime);

    List<AiModelUsageShareVO> modelUsageShare(String startTime, String endTime);
}

