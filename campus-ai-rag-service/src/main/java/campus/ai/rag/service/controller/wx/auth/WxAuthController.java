package campus.ai.rag.service.controller.wx.auth;

import campus.ai.rag.service.common.Result;
import campus.ai.rag.service.dto.wx.WxEmailCodeDTO;
import campus.ai.rag.service.dto.wx.WxLoginDTO;
import campus.ai.rag.service.dto.wx.WxNicknameUpdateDTO;
import campus.ai.rag.service.dto.wx.WxPasswordUpdateDTO;
import campus.ai.rag.service.dto.wx.WxRegisterDTO;
import campus.ai.rag.service.service.wx.auth.WxAuthService;
import campus.ai.rag.service.vo.admin.AdminLoginVO;
import campus.ai.rag.service.vo.wx.WxEmailCodeVO;
import campus.ai.rag.service.vo.wx.WxPowerPointLogPageVO;
import campus.ai.rag.service.vo.wx.WxProfileVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

    @PostMapping("/password-code")
    @ApiOperation(value = "发送修改密码验证码", notes = "已登录用户向当前账号邮箱发送验证码")
    @Operation(summary = "发送修改密码验证码")
    public Result<WxEmailCodeVO> sendPasswordCode() {
        return Result.success(wxAuthService.sendPasswordUpdateCode());
    }

    @PostMapping("/password/update")
    @ApiOperation(value = "修改密码", notes = "支持 EMAIL_CODE 与 WX_ONE_CLICK 两种校验")
    @Operation(summary = "修改密码")
    public Result<Boolean> updatePassword(@RequestBody WxPasswordUpdateDTO dto) {
        return Result.success(wxAuthService.updatePassword(dto));
    }

    @GetMapping("/profile")
    @ApiOperation(value = "获取个人资料")
    @Operation(summary = "获取个人资料")
    public Result<WxProfileVO> profile() {
        return Result.success(wxAuthService.getProfile());
    }

    @GetMapping("/power-points/logs")
    @ApiOperation(value = "获取算力点扣费明细")
    @Operation(summary = "获取算力点扣费明细")
    public Result<WxPowerPointLogPageVO> powerPointLogs(
            @RequestParam(value = "pageNum", defaultValue = "1") Long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") Long pageSize) {
        return Result.success(wxAuthService.listPowerPointLogs(pageNum, pageSize));
    }

    @PostMapping("/avatar/upload")
    @ApiOperation(value = "上传头像")
    @Operation(summary = "上传头像")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return Result.success(wxAuthService.uploadAvatar(file));
    }

    @PostMapping("/nickname/update")
    @ApiOperation(value = "修改昵称")
    @Operation(summary = "修改昵称")
    public Result<Boolean> updateNickname(@RequestBody WxNicknameUpdateDTO dto) {
        return Result.success(wxAuthService.updateNickname(dto));
    }
}

