package campus.ai.rag.service.service.wx.pay;

import campus.ai.rag.service.dto.wx.WxAlipayCreateOrderDTO;
import campus.ai.rag.service.vo.wx.WxAlipayCreateOrderVO;
import campus.ai.rag.service.vo.wx.WxRechargeOrderVO;

import java.util.List;
import java.util.Map;

public interface WxAlipayPayService {

    WxAlipayCreateOrderVO createWapOrder(WxAlipayCreateOrderDTO dto);

    String buildPayUrlByOrderNo(String orderNo);

    String handleNotify(Map<String, String> params);

    WxRechargeOrderVO getMyOrder(String orderNo);

    List<WxRechargeOrderVO> listMyOrders();
}

