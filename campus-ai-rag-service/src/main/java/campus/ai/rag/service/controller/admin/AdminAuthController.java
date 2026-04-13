package campus.ai.rag.service.controller.admin;

import campus.ai.rag.service.common.Result;
import campus.ai.rag.service.dto.admin.AdminLoginDTO;
import campus.ai.rag.service.service.admin.auth.AdminAuthService;
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
 * 后台认证接口。
 */
@RestController
@RequestMapping("/admin/auth")
@Api(tags = "后台认证接口")
@Tag(name = "后台认证")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    public AdminAuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @PostMapping("/login")
    @ApiOperation(value = "后台账号登录", notes = "用户名密码登录，成功后返回Token")
    @ApiImplicitParam(name = "dto", value = "后台登录参数", required = true, dataType = "AdminLoginDTO", paramType = "body")
    @Operation(summary = "后台账号登录")
    public Result<AdminLoginVO> login(@RequestBody AdminLoginDTO dto) {
        return Result.success(adminAuthService.login(dto));
    }
}

