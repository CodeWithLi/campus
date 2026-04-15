package campus.ai.rag.service.controller.admin.kb;

import campus.ai.rag.service.common.Result;
import campus.ai.rag.service.dto.admin.kb.KbCategoryCreateDTO;
import campus.ai.rag.service.dto.admin.kb.KbCategoryUpdateDTO;
import campus.ai.rag.service.service.admin.kb.AdminKbCategoryService;
import campus.ai.rag.service.vo.admin.kb.KbCategoryVO;
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
@RequestMapping("/admin/kb/categories")
@Api(tags = "后台知识库分类接口")
@Tag(name = "后台知识库分类")
@PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ROLE_SUPER_ADMIN','ROLE_ADMIN')")
public class AdminKbCategoryController {

    private final AdminKbCategoryService adminKbCategoryService;

    public AdminKbCategoryController(AdminKbCategoryService adminKbCategoryService) {
        this.adminKbCategoryService = adminKbCategoryService;
    }

    @GetMapping
    @ApiOperation(value = "查询分类列表", notes = "管理员与超级管理员可查询知识库分类")
    @Operation(summary = "查询分类列表")
    public Result<List<KbCategoryVO>> list() {
        return Result.success(adminKbCategoryService.listAll());
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "按ID查询分类", notes = "管理员与超级管理员可按ID查询知识库分类")
    @ApiImplicitParam(name = "id", value = "分类ID", required = true, dataType = "long", paramType = "path")
    @Operation(summary = "按ID查询分类")
    public Result<KbCategoryVO> getById(@PathVariable("id") Long id) {
        return Result.success(adminKbCategoryService.getById(id));
    }

    @PostMapping
    @ApiOperation(value = "新增分类", notes = "管理员与超级管理员可新增知识库分类")
    @ApiImplicitParam(name = "dto", value = "新增分类参数", required = true, dataType = "KbCategoryCreateDTO", paramType = "body")
    @Operation(summary = "新增分类")
    public Result<KbCategoryVO> create(@RequestBody KbCategoryCreateDTO dto) {
        return Result.success(adminKbCategoryService.create(dto));
    }

    @PutMapping("/{id}")
    @ApiOperation(value = "更新分类", notes = "管理员与超级管理员可更新知识库分类")
    @ApiImplicitParam(name = "id", value = "分类ID", required = true, dataType = "long", paramType = "path")
    @Operation(summary = "更新分类")
    public Result<KbCategoryVO> update(@PathVariable("id") Long id, @RequestBody KbCategoryUpdateDTO dto) {
        return Result.success(adminKbCategoryService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @ApiOperation(value = "删除分类", notes = "管理员与超级管理员可删除知识库分类")
    @ApiImplicitParam(name = "id", value = "分类ID", required = true, dataType = "long", paramType = "path")
    @Operation(summary = "删除分类")
    public Result<Void> delete(@PathVariable("id") Long id) {
        adminKbCategoryService.delete(id);
        return Result.success();
    }
}

