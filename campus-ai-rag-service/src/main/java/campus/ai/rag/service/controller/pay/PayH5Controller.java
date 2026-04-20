package campus.ai.rag.service.controller.pay;

import campus.ai.rag.service.exception.BusinessException;
import campus.ai.rag.service.service.wx.pay.WxAlipayPayService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pay")
public class PayH5Controller {

    private final WxAlipayPayService wxAlipayPayService;

    public PayH5Controller(WxAlipayPayService wxAlipayPayService) {
        this.wxAlipayPayService = wxAlipayPayService;
    }

    @GetMapping(value = "/h5", produces = MediaType.TEXT_HTML_VALUE)
    public String h5(@RequestParam("orderNo") String orderNo) {
        try {
            String payUrl = wxAlipayPayService.buildPayUrlByOrderNo(orderNo);
            return buildHtml(payUrl);
        } catch (BusinessException ex) {
            return buildErrorHtml(ex.getMessage());
        } catch (Exception ex) {
            return buildErrorHtml("支付页加载失败，请返回小程序重试");
        }
    }

    private String buildHtml(String payUrl) {
        String escaped = escapeHtml(payUrl);
        return """
                <!doctype html>
                <html lang="zh-CN">
                <head>
                  <meta charset="UTF-8"/>
                  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
                  <title>跳转支付宝支付</title>
                  <style>
                    body{margin:0;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Arial;background:#f6f8fc;color:#1f2937}
                    .wrap{max-width:560px;margin:0 auto;padding:36px 20px}
                    .card{background:#fff;border-radius:14px;padding:24px;box-shadow:0 8px 28px rgba(15,23,42,.08)}
                    .title{font-size:20px;font-weight:700;margin:0 0 8px}
                    .desc{font-size:14px;color:#6b7280;line-height:1.7}
                    .btn{display:block;margin-top:18px;padding:12px 14px;border-radius:10px;text-align:center;text-decoration:none;background:#10b981;color:#fff;font-weight:700}
                    .tip{margin-top:14px;font-size:12px;color:#9ca3af}
                  </style>
                </head>
                <body>
                  <div class="wrap">
                    <div class="card">
                      <p class="title">正在跳转支付宝...</p>
                      <p class="desc">若未自动跳转，请点击下方按钮继续支付。</p>
                      <a class="btn" href="%s">继续支付</a>
                      <p class="tip">提示：在微信内置环境可能无法直接唤起支付宝，可复制链接到外部浏览器打开。</p>
                    </div>
                  </div>
                  <script>
                    setTimeout(function(){ window.location.href = %s; }, 200);
                  </script>
                </body>
                </html>
                """.formatted(escaped, toJsStringLiteral(payUrl));
    }

    private String buildErrorHtml(String message) {
        return """
                <!doctype html>
                <html lang="zh-CN">
                <head><meta charset="UTF-8"/><meta name="viewport" content="width=device-width, initial-scale=1.0"/><title>支付页异常</title></head>
                <body style="font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Arial;background:#f6f8fc;padding:24px;color:#1f2937">
                  <div style="max-width:560px;margin:0 auto;background:#fff;border-radius:14px;padding:20px;box-shadow:0 8px 28px rgba(15,23,42,.08)">
                    <h3 style="margin:0 0 8px">支付页加载失败</h3>
                    <p style="margin:0;color:#6b7280;line-height:1.7">%s</p>
                  </div>
                </body>
                </html>
                """.formatted(escapeHtml(message == null ? "未知错误" : message));
    }

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String toJsStringLiteral(String value) {
        if (value == null) return "''";
        String escaped = value.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\r", "")
                .replace("\n", "");
        return "'" + escaped + "'";
    }
}

