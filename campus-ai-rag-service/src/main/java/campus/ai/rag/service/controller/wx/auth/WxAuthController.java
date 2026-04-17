package campus.ai.rag.service.controller.wx.auth;

import campus.ai.rag.service.common.Result;
import campus.ai.rag.service.dto.wx.WxEmailCodeDTO;
import campus.ai.rag.service.dto.wx.WxLoginDTO;
import campus.ai.rag.service.dto.wx.WxRegisterDTO;
import campus.ai.rag.service.service.wx.auth.WxAuthService;
import campus.ai.rag.service.vo.admin.AdminLoginVO;
import campus.ai.rag.service.vo.wx.WxEmailCodeVO;
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
 * 微信端认证接口：发送邮箱验证码 + 用户注册。
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

    @PostMapping("/email-code")
    @ApiOperation(value = "获取邮箱验证码", notes = "发送6位验证码到邮箱并写入Redis，注册时进行校验")
    @ApiImplicitParam(name = "dto", value = "获取邮箱验证码参数", required = true, dataType = "WxEmailCodeDTO", paramType = "body")
    @Operation(summary = "获取邮箱验证码")
    public Result<WxEmailCodeVO> sendEmailCode(@RequestBody WxEmailCodeDTO dto) {
        return Result.success(wxAuthService.sendEmailCode(dto));
    }

    @PostMapping("/register")
    @ApiOperation(value = "用户注册", notes = "用户名+密码+邮箱+邮箱验证码注册，并通过wxCode绑定openid后返回登录Token")
    @ApiImplicitParam(name = "dto", value = "用户注册参数", required = true, dataType = "WxRegisterDTO", paramType = "body")
    @Operation(summary = "用户注册")
    public Result<AdminLoginVO> register(@RequestBody WxRegisterDTO dto) {
        return Result.success(wxAuthService.register(dto));
    }

    @PostMapping("/login")
    @ApiOperation(value = "用户登录", notes = "策略模式登录：支持PASSWORD(用户名密码)和WX_ONE_CLICK(微信一键登录)")
    @ApiImplicitParam(name = "dto", value = "用户登录参数", required = true, dataType = "WxLoginDTO", paramType = "body")
    @Operation(summary = "用户登录")
    public Result<AdminLoginVO> login(@RequestBody WxLoginDTO dto) {
        return Result.success(wxAuthService.login(dto));
    }
}

