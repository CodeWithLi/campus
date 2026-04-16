package campus.ai.rag.service.controller.admin.kb;

import campus.ai.rag.service.common.Result;
import campus.ai.rag.service.service.admin.kb.AdminWxKbDocumentService;
import campus.ai.rag.service.vo.admin.kb.AdminWxKbDocumentPageVO;
import campus.ai.rag.service.vo.admin.kb.AdminWxKbDocumentPreviewVO;
import campus.ai.rag.service.vo.admin.kb.AdminWxKbDocumentVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/wx-kb/documents")
@Api(tags = "后台用户个人知识库文档接口")
@Tag(name = "后台用户个人知识库文档")
@PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ROLE_SUPER_ADMIN','ROLE_ADMIN')")
public class AdminWxKbDocumentController {

    private final AdminWxKbDocumentService adminWxKbDocumentService;

    public AdminWxKbDocumentController(AdminWxKbDocumentService adminWxKbDocumentService) {
        this.adminWxKbDocumentService = adminWxKbDocumentService;
    }

    @GetMapping
    @ApiOperation(value = "分页查询用户上传文档", notes = "管理员与超级管理员可分页查看用户个人知识库文档")
    @Operation(summary = "分页查询用户上传文档")
    public Result<AdminWxKbDocumentPageVO> list(@RequestParam(value = "pageNum", defaultValue = "1") Long pageNum,
                                                @RequestParam(value = "pageSize", defaultValue = "10") Long pageSize,
                                                @RequestParam(value = "userId", required = false) Long userId) {
        return Result.success(adminWxKbDocumentService.listPage(pageNum, pageSize, userId));
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "按ID查询用户上传文档", notes = "管理员与超级管理员可查看文档详情")
    @ApiImplicitParam(name = "id", value = "文档ID", required = true, dataType = "long", paramType = "path")
    @Operation(summary = "按ID查询用户上传文档")
    public Result<AdminWxKbDocumentVO> getById(@PathVariable("id") Long id) {
        return Result.success(adminWxKbDocumentService.getById(id));
    }

    @GetMapping("/{id}/preview")
    @ApiOperation(value = "预览用户上传文档文本", notes = "优先返回已解析文本；若还未持久化解析文本，则现场解析预览")
    @ApiImplicitParam(name = "id", value = "文档ID", required = true, dataType = "long", paramType = "path")
    @Operation(summary = "预览用户上传文档文本")
    public Result<AdminWxKbDocumentPreviewVO> preview(@PathVariable("id") Long id) {
        return Result.success(adminWxKbDocumentService.preview(id));
    }
}

