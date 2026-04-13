package campus.ai.rag.service.controller.wx.auth;

import campus.ai.rag.service.common.Result;
import campus.ai.rag.service.dto.wx.WxLoginDTO;
import campus.ai.rag.service.service.wx.auth.WxAuthService;
import campus.ai.rag.service.vo.admin.AdminLoginVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 微信端登录接口：账号密码登录，成功后颁发 Redis Token。
 */
@RestController
@RequestMapping("/wx/auth")
@Api(tags = "微信端认证接口")
@Tag(name = "微信端认证")
public class WxAuthController {

    private final WxAuthService wxAuthService;

    public WxAuthController(WxAuthService wxAuthService) {
        this.wxAuthService = wxAuthService;
    }

    @PostMapping("/login")
    @ApiOperation(value = "微信端登录", notes = "微信端账号密码登录，成功后返回Token")
    @ApiImplicitParam(name = "dto", value = "微信端登录参数", required = true, dataType = "WxLoginDTO", paramType = "body")
    @Operation(summary = "微信端登录")
    public Result<AdminLoginVO> login(@RequestBody WxLoginDTO dto) {
        return Result.success(wxAuthService.login(dto));
    }
}

