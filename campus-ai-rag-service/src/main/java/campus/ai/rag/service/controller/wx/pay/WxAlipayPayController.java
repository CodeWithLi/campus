package campus.ai.rag.service.controller.wx.pay;

import campus.ai.rag.service.common.Result;
import campus.ai.rag.service.dto.wx.WxAlipayCreateOrderDTO;
import campus.ai.rag.service.service.wx.pay.WxAlipayPayService;
import campus.ai.rag.service.vo.wx.WxAlipayCreateOrderVO;
import campus.ai.rag.service.vo.wx.WxRechargeOrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/wx/pay/alipay")
@Api(tags = "微信端支付宝支付")
@Tag(name = "微信端支付宝支付")
public class WxAlipayPayController {

    private final WxAlipayPayService wxAlipayPayService;

    public WxAlipayPayController(WxAlipayPayService wxAlipayPayService) {
        this.wxAlipayPayService = wxAlipayPayService;
    }

    @PostMapping("/wap/create")
    @ApiOperation(value = "创建支付宝WAP支付订单")
    @Operation(summary = "创建支付宝WAP支付订单")
    public Result<WxAlipayCreateOrderVO> create(@RequestBody WxAlipayCreateOrderDTO dto) {
        return Result.success(wxAlipayPayService.createWapOrder(dto));
    }

    @PostMapping("/notify")
    @ApiOperation(value = "支付宝异步回调")
    @Operation(summary = "支付宝异步回调")
    public String notifyUrl(HttpServletRequest request) {
        Map<String, String[]> paramMap = request.getParameterMap();
        Map<String, String> params = new HashMap<>();
        for (Map.Entry<String, String[]> entry : paramMap.entrySet()) {
            String[] values = entry.getValue();
            if (values != null && values.length > 0) {
                params.put(entry.getKey(), values[0]);
            }
        }
        return wxAlipayPayService.handleNotify(params);
    }

    @GetMapping("/orders")
    @ApiOperation(value = "查询我的充值订单列表")
    @Operation(summary = "查询我的充值订单列表")
    public Result<List<WxRechargeOrderVO>> listMyOrders() {
        return Result.success(wxAlipayPayService.listMyOrders());
    }

    @GetMapping("/orders/{orderNo}")
    @ApiOperation(value = "查询我的充值订单详情")
    @Operation(summary = "查询我的充值订单详情")
    public Result<WxRechargeOrderVO> myOrder(@PathVariable("orderNo") String orderNo) {
        return Result.success(wxAlipayPayService.getMyOrder(orderNo));
    }

    @GetMapping("/return")
    @ApiOperation(value = "支付宝同步跳转地址")
    @Operation(summary = "支付宝同步跳转地址")
    public String returnPage() {
        return "支付结果请返回小程序查看";
    }
}

