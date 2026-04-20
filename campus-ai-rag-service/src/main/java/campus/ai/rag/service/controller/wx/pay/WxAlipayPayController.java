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
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
        log.info("[ALIPAY_NOTIFY] receive callback, out_trade_no={}, trade_no={}, trade_status={}, total_amount={}",
                params.get("out_trade_no"),
                params.get("trade_no"),
                params.get("trade_status"),
                params.get("total_amount"));
        String result = wxAlipayPayService.handleNotify(params);
        log.info("[ALIPAY_NOTIFY] callback handled, out_trade_no={}, result={}", params.get("out_trade_no"), result);
        return result;
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

    @PostMapping("/orders/{orderNo}/query")
    @ApiOperation(value = "主动查询并同步订单状态")
    @Operation(summary = "主动查询并同步订单状态")
    public Result<WxRechargeOrderVO> queryOrder(@PathVariable("orderNo") String orderNo) {
        return Result.success(wxAlipayPayService.queryAndSyncMyOrder(orderNo));
    }

    @GetMapping("/return")
    @ApiOperation(value = "支付宝同步跳转地址")
    @Operation(summary = "支付宝同步跳转地址")
    public String returnPage() {
        return """
                <!doctype html>
                <html lang="zh-CN">
                <head><meta charset="UTF-8"/><meta name="viewport" content="width=device-width, initial-scale=1.0"/><title>支付结果</title></head>
                <body style="margin:0;background:#f6f8fc;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Arial;padding:24px;">
                  <div style="max-width:560px;margin:0 auto;background:#fff;border-radius:14px;padding:20px;box-shadow:0 8px 28px rgba(15,23,42,.08);">
                    <h3 style="margin:0 0 8px;">支付已返回</h3>
                    <p style="margin:0;color:#64748b;line-height:1.7;">订单结果以异步回调为准，请回到小程序刷新余额或查看充值记录。</p>
                  </div>
                </body>
                </html>
                """;
    }
}

