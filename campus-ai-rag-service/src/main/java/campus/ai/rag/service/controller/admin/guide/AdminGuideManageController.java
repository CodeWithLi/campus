package campus.ai.rag.service.controller.admin.guide;

import campus.ai.rag.service.common.Result;
import campus.ai.rag.service.dto.admin.guide.AdminGuideSaveDTO;
import campus.ai.rag.service.service.admin.guide.AdminGuideManageService;
import campus.ai.rag.service.vo.admin.guide.AdminGuidePageVO;
import campus.ai.rag.service.vo.admin.guide.AdminGuideVO;
import io.swagger.annotations.Api;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/admin/guides")
@Api(tags = "后台办事指南管理")
@Tag(name = "后台办事指南管理")
@PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ROLE_SUPER_ADMIN','ROLE_ADMIN')")
public class AdminGuideManageController {

    private final AdminGuideManageService adminGuideManageService;

    public AdminGuideManageController(AdminGuideManageService adminGuideManageService) {
        this.adminGuideManageService = adminGuideManageService;
    }

    @GetMapping
    @ApiOperation(value = "分页查询指南", notes = "支持关键词、状态、分类筛选")
    @Operation(summary = "分页查询指南")
    public Result<AdminGuidePageVO> list(
            @RequestParam(value = "pageNum", defaultValue = "1") Long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") Long pageSize,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "category", required = false) String category) {
        return Result.success(adminGuideManageService.listPage(pageNum, pageSize, keyword, status, category));
    }

    @GetMapping("/{guideId}")
    @ApiOperation(value = "查询指南详情", notes = "返回基础信息与步骤列表")
    @Operation(summary = "查询指南详情")
    public Result<AdminGuideVO> detail(@PathVariable("guideId") Long guideId) {
        return Result.success(adminGuideManageService.detail(guideId));
    }

    @PostMapping
    @ApiOperation(value = "新增指南", notes = "默认创建为草稿状态")
    @Operation(summary = "新增指南")
    public Result<AdminGuideVO> create(@RequestBody AdminGuideSaveDTO dto) {
        return Result.success(adminGuideManageService.create(dto));
    }

    @PutMapping("/{guideId}")
    @ApiOperation(value = "更新指南", notes = "会覆盖步骤配置")
    @Operation(summary = "更新指南")
    public Result<AdminGuideVO> update(@PathVariable("guideId") Long guideId, @RequestBody AdminGuideSaveDTO dto) {
        return Result.success(adminGuideManageService.update(guideId, dto));
    }

    @PostMapping("/{guideId}/publish")
    @ApiOperation(value = "上架指南", notes = "状态切换为 PUBLISHED")
    @Operation(summary = "上架指南")
    public Result<Boolean> publish(@PathVariable("guideId") Long guideId) {
        adminGuideManageService.publish(guideId);
        return Result.success(true);
    }

    @PostMapping("/{guideId}/offline")
    @ApiOperation(value = "下架指南", notes = "状态切换为 OFFLINE")
    @Operation(summary = "下架指南")
    public Result<Boolean> offline(@PathVariable("guideId") Long guideId) {
        adminGuideManageService.offline(guideId);
        return Result.success(true);
    }

    @PostMapping(value = "/cover/upload", consumes = {"multipart/form-data"})
    @ApiOperation(value = "上传指南封面图", notes = "文件存储到 upload/guide 目录")
    @Operation(summary = "上传指南封面图")
    public Result<String> uploadCover(@RequestParam("file") MultipartFile file) {
        return Result.success(adminGuideManageService.uploadCover(file));
    }

    @DeleteMapping("/{guideId}")
    @ApiOperation(value = "删除指南", notes = "逻辑删除指南并删除步骤")
    @Operation(summary = "删除指南")
    public Result<Boolean> delete(@PathVariable("guideId") Long guideId) {
        adminGuideManageService.delete(guideId);
        return Result.success(true);
    }
}
