package campus.ai.rag.service.controller.admin.prompt;

import campus.ai.rag.service.common.Result;
import campus.ai.rag.service.dto.admin.prompt.AiSystemPromptCreateDTO;
import campus.ai.rag.service.dto.admin.prompt.AiSystemPromptUpdateDTO;
import campus.ai.rag.service.service.admin.prompt.AdminSystemPromptService;
import campus.ai.rag.service.vo.admin.prompt.AiSystemPromptVO;
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
@RequestMapping("/admin/prompts/system")
@Api(tags = "后台系统提示词接口")
@Tag(name = "后台系统提示词")
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
public class AdminSystemPromptController {

    private final AdminSystemPromptService adminSystemPromptService;

    public AdminSystemPromptController(AdminSystemPromptService adminSystemPromptService) {
        this.adminSystemPromptService = adminSystemPromptService;
    }

    @GetMapping
    @ApiOperation(value = "查询系统提示词列表", notes = "查询后台全部系统提示词")
    @Operation(summary = "查询系统提示词列表")
    public Result<List<AiSystemPromptVO>> list() {
        return Result.success(adminSystemPromptService.listAll());
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "按ID查询系统提示词", notes = "根据主键ID查询系统提示词")
    @ApiImplicitParam(name = "id", value = "系统提示词ID", required = true, dataType = "long", paramType = "path")
    @Operation(summary = "按ID查询系统提示词")
    public Result<AiSystemPromptVO> getById(@PathVariable("id") Long id) {
        return Result.success(adminSystemPromptService.getById(id));
    }

    @PostMapping
    @ApiOperation(value = "新增系统提示词", notes = "仅超级管理员可新增系统提示词")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @ApiImplicitParam(name = "dto", value = "新增系统提示词参数", required = true, dataType = "AiSystemPromptCreateDTO", paramType = "body")
    @Operation(summary = "新增系统提示词")
    public Result<AiSystemPromptVO> create(@RequestBody AiSystemPromptCreateDTO dto) {
        return Result.success(adminSystemPromptService.create(dto));
    }

    @PutMapping("/{id}")
    @ApiOperation(value = "更新系统提示词", notes = "仅超级管理员可更新系统提示词")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @ApiImplicitParam(name = "id", value = "系统提示词ID", required = true, dataType = "long", paramType = "path")
    @Operation(summary = "更新系统提示词")
    public Result<AiSystemPromptVO> update(@PathVariable("id") Long id, @RequestBody AiSystemPromptUpdateDTO dto) {
        return Result.success(adminSystemPromptService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @ApiOperation(value = "删除系统提示词", notes = "仅超级管理员可删除系统提示词")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @ApiImplicitParam(name = "id", value = "系统提示词ID", required = true, dataType = "long", paramType = "path")
    @Operation(summary = "删除系统提示词")
    public Result<Void> delete(@PathVariable("id") Long id) {
        adminSystemPromptService.delete(id);
        return Result.success();
    }
}

