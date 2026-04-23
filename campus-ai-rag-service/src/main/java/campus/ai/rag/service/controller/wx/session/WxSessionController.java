package campus.ai.rag.service.controller.wx.session;

import campus.ai.rag.service.common.Result;
import campus.ai.rag.service.utils.WxAuthContextUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 微信端会话接口：用于前端主动探测 token 是否过期。
 */
@RestController
@RequestMapping("/wx/session")
@Api(tags = "微信端会话接口")
@Tag(name = "微信端会话")
public class WxSessionController {

    @GetMapping("/check")
    @ApiOperation(value = "检查会话是否有效")
    @Operation(summary = "检查会话是否有效")
    public Result<Boolean> check() {
        // 只要能拿到 userId，说明 token 有效
        WxAuthContextUtils.requireWxUserId();
        return Result.success(true);
    }
}

