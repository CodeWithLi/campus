package campus.ai.rag.service.controller.admin.user;

import campus.ai.rag.service.common.Result;
import campus.ai.rag.service.dto.admin.wxuser.AdminWxUserPowerPointsDTO;
import campus.ai.rag.service.dto.admin.wxuser.AdminWxUserUpdateDTO;
import campus.ai.rag.service.service.admin.wxuser.AdminWxUserManageService;
import campus.ai.rag.service.vo.admin.wxuser.AdminWxUserVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/wx-users")
@Api(tags = "后台微信用户管理接口")
@Tag(name = "后台微信用户管理")
@PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ROLE_SUPER_ADMIN','ROLE_ADMIN')")
public class AdminWxUserManageController {

    private final AdminWxUserManageService adminWxUserManageService;

    public AdminWxUserManageController(AdminWxUserManageService adminWxUserManageService) {
        this.adminWxUserManageService = adminWxUserManageService;
    }

    @GetMapping
    @ApiOperation(value = "查询微信用户列表", notes = "管理员与超级管理员可查询微信用户列表")
    @Operation(summary = "查询微信用户列表")
    public Result<List<AdminWxUserVO>> list() {
        return Result.success(adminWxUserManageService.listAll());
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "按ID查询微信用户", notes = "管理员与超级管理员可按ID查询微信用户")
    @ApiImplicitParam(name = "id", value = "微信用户ID", required = true, dataType = "long", paramType = "path")
    @Operation(summary = "按ID查询微信用户")
    public Result<AdminWxUserVO> getById(@PathVariable("id") Long id) {
        return Result.success(adminWxUserManageService.getById(id));
    }

    @PutMapping("/{id}")
    @ApiOperation(value = "更新微信用户信息", notes = "管理员与超级管理员可更新昵称/邮箱/状态/算力点")
    @ApiImplicitParam(name = "id", value = "微信用户ID", required = true, dataType = "long", paramType = "path")
    @Operation(summary = "更新微信用户信息")
    public Result<AdminWxUserVO> update(@PathVariable("id") Long id, @RequestBody AdminWxUserUpdateDTO dto) {
        return Result.success(adminWxUserManageService.update(id, dto));
    }

    @PutMapping("/{id}/power-points")
    @ApiOperation(value = "修改用户算力点", notes = "管理员与超级管理员可直接设置用户算力点")
    @ApiImplicitParam(name = "id", value = "微信用户ID", required = true, dataType = "long", paramType = "path")
    @Operation(summary = "修改用户算力点")
    public Result<AdminWxUserVO> updatePowerPoints(@PathVariable("id") Long id, @RequestBody AdminWxUserPowerPointsDTO dto) {
        return Result.success(adminWxUserManageService.updatePowerPoints(id, dto));
    }

    @PutMapping("/{id}/freeze")
    @ApiOperation(value = "冻结微信用户", notes = "管理员与超级管理员可冻结微信用户")
    @ApiImplicitParam(name = "id", value = "微信用户ID", required = true, dataType = "long", paramType = "path")
    @Operation(summary = "冻结微信用户")
    public Result<Void> freeze(@PathVariable("id") Long id) {
        adminWxUserManageService.freeze(id);
        return Result.success();
    }

    @PutMapping("/{id}/unfreeze")
    @ApiOperation(value = "解冻微信用户", notes = "管理员与超级管理员可解冻微信用户")
    @ApiImplicitParam(name = "id", value = "微信用户ID", required = true, dataType = "long", paramType = "path")
    @Operation(summary = "解冻微信用户")
    public Result<Void> unfreeze(@PathVariable("id") Long id) {
        adminWxUserManageService.unfreeze(id);
        return Result.success();
    }
}

