package campus.ai.rag.service.controller.admin.model;

import campus.ai.rag.service.common.Result;
import campus.ai.rag.service.dto.admin.model.AiModelConfigCreateDTO;
import campus.ai.rag.service.dto.admin.model.AiModelConfigUpdateDTO;
import campus.ai.rag.service.dto.admin.model.AiModelTestChatDTO;
import campus.ai.rag.service.service.admin.model.AdminModelConfigService;
import campus.ai.rag.service.vo.admin.model.AiModelConfigVO;
import campus.ai.rag.service.vo.admin.model.AiModelTestChatVO;
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
@RequestMapping("/admin/models")
@Api(tags = "后台模型配置接口")
@Tag(name = "后台模型配置")
public class AdminModelConfigController {

    private final AdminModelConfigService adminModelConfigService;

    public AdminModelConfigController(AdminModelConfigService adminModelConfigService) {
        this.adminModelConfigService = adminModelConfigService;
    }

    @GetMapping
    @ApiOperation(value = "查询模型配置列表", notes = "查询后台全部模型配置")
    @Operation(summary = "查询模型配置列表")
    public Result<List<AiModelConfigVO>> list() {
        return Result.success(adminModelConfigService.listAll());
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "按ID查询模型配置", notes = "根据主键ID查询模型配置")
    @ApiImplicitParam(name = "id", value = "模型配置ID", required = true, dataType = "long", paramType = "path")
    @Operation(summary = "按ID查询模型配置")
    public Result<AiModelConfigVO> getById(@PathVariable("id") Long id) {
        return Result.success(adminModelConfigService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @ApiOperation(value = "新增模型配置", notes = "仅超级管理员可新增模型配置")
    @ApiImplicitParam(name = "dto", value = "新增模型配置参数", required = true, dataType = "AiModelConfigCreateDTO", paramType = "body")
    @Operation(summary = "新增模型配置")
    public Result<AiModelConfigVO> create(@RequestBody AiModelConfigCreateDTO dto) {
        return Result.success(adminModelConfigService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @ApiOperation(value = "更新模型配置", notes = "仅超级管理员可更新模型配置")
    @ApiImplicitParam(name = "id", value = "模型配置ID", required = true, dataType = "long", paramType = "path")
    @Operation(summary = "更新模型配置")
    public Result<AiModelConfigVO> update(@PathVariable("id") Long id, @RequestBody AiModelConfigUpdateDTO dto) {
        return Result.success(adminModelConfigService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @ApiOperation(value = "删除模型配置", notes = "仅超级管理员可删除模型配置")
    @ApiImplicitParam(name = "id", value = "模型配置ID", required = true, dataType = "long", paramType = "path")
    @Operation(summary = "删除模型配置")
    public Result<Void> delete(@PathVariable("id") Long id) {
        adminModelConfigService.delete(id);
        return Result.success();
    }

    @PostMapping("/test-chat")
    @ApiOperation(value = "模型连通性测试", notes = "按模型编码发起一次测试问答，返回模型回复与耗时")
    @Operation(summary = "模型连通性测试")
    public Result<AiModelTestChatVO> testChat(@RequestBody AiModelTestChatDTO dto) {
        return Result.success(adminModelConfigService.testChat(dto));
    }
}

