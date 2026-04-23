package campus.ai.rag.service.controller.admin.guide;

import campus.ai.rag.service.common.Result;
import campus.ai.rag.service.dto.admin.guide.AdminMaterialTemplateSaveDTO;
import campus.ai.rag.service.service.admin.guide.AdminMaterialTemplateService;
import campus.ai.rag.service.vo.admin.guide.AdminMaterialTemplateVO;
import io.swagger.annotations.Api;
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
@RequestMapping("/admin/guides/material-templates")
@Api(tags = "后台材料模板管理")
@Tag(name = "后台材料模板管理")
@PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ROLE_SUPER_ADMIN','ROLE_ADMIN')")
public class AdminMaterialTemplateController {

    private final AdminMaterialTemplateService adminMaterialTemplateService;

    public AdminMaterialTemplateController(AdminMaterialTemplateService adminMaterialTemplateService) {
        this.adminMaterialTemplateService = adminMaterialTemplateService;
    }

    @GetMapping
    @Operation(summary = "材料模板列表")
    public Result<List<AdminMaterialTemplateVO>> list() {
        return Result.success(adminMaterialTemplateService.listAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "材料模板详情")
    public Result<AdminMaterialTemplateVO> detail(@PathVariable("id") Long id) {
        return Result.success(adminMaterialTemplateService.getById(id));
    }

    @PostMapping
    @Operation(summary = "新增材料模板")
    public Result<AdminMaterialTemplateVO> create(@RequestBody AdminMaterialTemplateSaveDTO dto) {
        return Result.success(adminMaterialTemplateService.create(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新材料模板")
    public Result<AdminMaterialTemplateVO> update(@PathVariable("id") Long id, @RequestBody AdminMaterialTemplateSaveDTO dto) {
        return Result.success(adminMaterialTemplateService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除材料模板")
    public Result<Boolean> delete(@PathVariable("id") Long id) {
        adminMaterialTemplateService.delete(id);
        return Result.success(true);
    }
}

