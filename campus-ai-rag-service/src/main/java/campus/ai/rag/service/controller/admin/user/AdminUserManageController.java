package campus.ai.rag.service.controller.admin.user;

import campus.ai.rag.service.common.Result;
import campus.ai.rag.service.dto.admin.user.AdminUserCreateDTO;
import campus.ai.rag.service.dto.admin.user.AdminUserResetPasswordDTO;
import campus.ai.rag.service.dto.admin.user.AdminUserUpdateDTO;
import campus.ai.rag.service.service.admin.user.AdminUserManageService;
import campus.ai.rag.service.vo.admin.user.AdminUserVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/users")
@Api(tags = "后台管理员管理接口")
@Tag(name = "后台管理员管理")
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
public class AdminUserManageController {

    private final AdminUserManageService adminUserManageService;

    public AdminUserManageController(AdminUserManageService adminUserManageService) {
        this.adminUserManageService = adminUserManageService;
    }

    @GetMapping
    @ApiOperation(value = "查询管理员列表", notes = "仅超级管理员可查询后台管理员列表")
    @Operation(summary = "查询管理员列表")
    public Result<List<AdminUserVO>> list() {
        return Result.success(adminUserManageService.listAll());
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "按ID查询管理员", notes = "仅超级管理员可按ID查询后台管理员")
    @ApiImplicitParam(name = "id", value = "管理员ID", required = true, dataType = "long", paramType = "path")
    @Operation(summary = "按ID查询管理员")
    public Result<AdminUserVO> getById(@PathVariable("id") Long id) {
        return Result.success(adminUserManageService.getById(id));
    }

    @PostMapping
    @ApiOperation(value = "新增管理员", notes = "仅超级管理员可新增后台管理员")
    @ApiImplicitParam(name = "dto", value = "新增管理员参数", required = true, dataType = "AdminUserCreateDTO", paramType = "body")
    @Operation(summary = "新增管理员")
    public Result<AdminUserVO> create(@RequestBody AdminUserCreateDTO dto) {
        return Result.success(adminUserManageService.create(dto));
    }

    @PutMapping("/{id}")
    @ApiOperation(value = "更新管理员", notes = "仅超级管理员可更新后台管理员信息")
    @ApiImplicitParam(name = "id", value = "管理员ID", required = true, dataType = "long", paramType = "path")
    @Operation(summary = "更新管理员")
    public Result<AdminUserVO> update(@PathVariable("id") Long id, @RequestBody AdminUserUpdateDTO dto) {
        return Result.success(adminUserManageService.update(id, dto));
    }

    @PutMapping("/{id}/reset-password")
    @ApiOperation(value = "重置管理员密码", notes = "仅超级管理员可重置后台管理员密码")
    @ApiImplicitParam(name = "id", value = "管理员ID", required = true, dataType = "long", paramType = "path")
    @Operation(summary = "重置管理员密码")
    public Result<Void> resetPassword(@PathVariable("id") Long id, @RequestBody AdminUserResetPasswordDTO dto) {
        adminUserManageService.resetPassword(id, dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @ApiOperation(value = "删除管理员", notes = "仅超级管理员可删除后台管理员")
    @ApiImplicitParam(name = "id", value = "管理员ID", required = true, dataType = "long", paramType = "path")
    @Operation(summary = "删除管理员")
    public Result<Void> delete(@PathVariable("id") Long id) {
        adminUserManageService.delete(id);
        return Result.success();
    }
}

