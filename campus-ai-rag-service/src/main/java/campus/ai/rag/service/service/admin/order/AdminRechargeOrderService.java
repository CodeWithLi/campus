package campus.ai.rag.service.service.admin.order;

import campus.ai.rag.service.vo.admin.order.AdminRechargeOrderPageVO;
import campus.ai.rag.service.vo.admin.order.AdminRechargeOrderVO;

public interface AdminRechargeOrderService {

    AdminRechargeOrderPageVO listPage(Long pageNum,
                                      Long pageSize,
                                      String orderNo,
                                      Long userId,
                                      String status,
                                      String channel,
                                      String startTime,
                                      String endTime);

    AdminRechargeOrderVO getById(Long id);
}
