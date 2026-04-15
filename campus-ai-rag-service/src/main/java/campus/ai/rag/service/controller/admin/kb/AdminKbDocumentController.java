package campus.ai.rag.service.controller.admin.kb;

import campus.ai.rag.service.common.Result;
import campus.ai.rag.service.dto.admin.kb.KbImageUnderstandIndexDTO;
import campus.ai.rag.service.dto.admin.kb.KbDocumentUpdateStatusDTO;
import campus.ai.rag.service.dto.admin.kb.KbDocumentUploadDTO;
import campus.ai.rag.service.service.admin.kb.AdminKbDocumentService;
import campus.ai.rag.service.vo.admin.kb.KbDocumentImagePreviewVO;
import campus.ai.rag.service.vo.admin.kb.KbImageUnderstandTaskVO;
import campus.ai.rag.service.vo.admin.kb.KbDocumentPageVO;
import campus.ai.rag.service.vo.admin.kb.KbDocumentVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/admin/kb/documents")
@Api(tags = "后台知识库文档接口")
@Tag(name = "后台知识库文档")
@PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ROLE_SUPER_ADMIN','ROLE_ADMIN')")
public class AdminKbDocumentController {

    private final AdminKbDocumentService adminKbDocumentService;

    public AdminKbDocumentController(AdminKbDocumentService adminKbDocumentService) {
        this.adminKbDocumentService = adminKbDocumentService;
    }

    @GetMapping
    @ApiOperation(value = "查询文档列表", notes = "管理员与超级管理员可查询知识库文档")
    @Operation(summary = "查询文档列表")
    public Result<KbDocumentPageVO> list(@RequestParam(value = "pageNum", defaultValue = "1") Long pageNum,
                                         @RequestParam(value = "pageSize", defaultValue = "10") Long pageSize) {
        return Result.success(adminKbDocumentService.listPage(pageNum, pageSize));
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "按ID查询文档", notes = "管理员与超级管理员可按ID查询知识库文档")
    @ApiImplicitParam(name = "id", value = "文档ID", required = true, dataType = "long", paramType = "path")
    @Operation(summary = "按ID查询文档")
    public Result<KbDocumentVO> getById(@PathVariable("id") Long id) {
        return Result.success(adminKbDocumentService.getById(id));
    }

    @PostMapping(value = "/upload", consumes = {"multipart/form-data"})
    @ApiOperation(value = "上传文档", notes = "上传文档并记录元数据，初始状态为PENDING")
    @Operation(summary = "上传文档")
    public Result<KbDocumentVO> upload(@RequestPart("meta") KbDocumentUploadDTO dto,
                                       @RequestPart("file") MultipartFile file) {
        return Result.success(adminKbDocumentService.upload(dto, file));
    }

    @PostMapping(value = "/upload-and-index", consumes = {"multipart/form-data"})
    @ApiOperation(value = "上传并一键解析向量化", notes = "上传文档后自动执行：解析 -> 切片向量化入库")
    @Operation(summary = "上传并一键解析向量化")
    public Result<KbDocumentVO> uploadAndIndex(@RequestPart("meta") KbDocumentUploadDTO dto,
                                               @RequestPart("file") MultipartFile file) {
        return Result.success(adminKbDocumentService.uploadAndIndex(dto, file));
    }

    @PutMapping("/{id}/status")
    @ApiOperation(value = "更新文档状态", notes = "用于处理流程中更新状态与失败原因")
    @ApiImplicitParam(name = "id", value = "文档ID", required = true, dataType = "long", paramType = "path")
    @Operation(summary = "更新文档状态")
    public Result<KbDocumentVO> updateStatus(@PathVariable("id") Long id,
                                             @RequestBody KbDocumentUpdateStatusDTO dto) {
        return Result.success(adminKbDocumentService.updateStatus(id, dto));
    }

    @PutMapping("/{id}/retry")
    @ApiOperation(value = "重试文档处理", notes = "将FAILED文档重置为PENDING并清空失败原因")
    @ApiImplicitParam(name = "id", value = "文档ID", required = true, dataType = "long", paramType = "path")
    @Operation(summary = "重试文档处理")
    public Result<KbDocumentVO> retry(@PathVariable("id") Long id) {
        return Result.success(adminKbDocumentService.retry(id));
    }

    @PutMapping("/{id}/parse")
    @ApiOperation(value = "解析文档文本", notes = "解析pdf/txt/md/doc/docx为纯文本，并更新处理状态")
    @ApiImplicitParam(name = "id", value = "文档ID", required = true, dataType = "long", paramType = "path")
    @Operation(summary = "解析文档文本")
    public Result<KbDocumentVO> parse(@PathVariable("id") Long id) {
        return Result.success(adminKbDocumentService.parse(id));
    }

    @PutMapping("/{id}/index")
    @ApiOperation(value = "切片并向量化入库", notes = "对已解析文档进行切片、千问向量化并写入pgvector")
    @ApiImplicitParam(name = "id", value = "文档ID", required = true, dataType = "long", paramType = "path")
    @Operation(summary = "切片并向量化入库")
    public Result<KbDocumentVO> index(@PathVariable("id") Long id) {
        return Result.success(adminKbDocumentService.index(id));
    }

    @PostMapping("/{id}/image-understand/preview")
    @ApiOperation(value = "图片理解预览", notes = "抽取文档图片并调用星火图片理解，供前端回显确认")
    @ApiImplicitParam(name = "id", value = "文档ID", required = true, dataType = "long", paramType = "path")
    @Operation(summary = "图片理解预览")
    public Result<KbDocumentImagePreviewVO> previewImageUnderstand(@PathVariable("id") Long id) {
        return Result.success(adminKbDocumentService.previewImageUnderstand(id));
    }

    @PostMapping("/{id}/image-understand/index")
    @ApiOperation(value = "图片理解后一键向量化", notes = "执行图片理解并与文档文本合并后，切片向量化入库")
    @ApiImplicitParam(name = "id", value = "文档ID", required = true, dataType = "long", paramType = "path")
    @Operation(summary = "图片理解后一键向量化")
    public Result<KbDocumentVO> imageUnderstandAndIndex(@PathVariable("id") Long id) {
        return Result.success(adminKbDocumentService.imageUnderstandAndIndex(id));
    }

    @PostMapping("/{id}/image-understand/preview-async")
    @ApiOperation(value = "异步提交图片理解预览", notes = "提交后立即返回taskId，前端可轮询任务状态")
    @ApiImplicitParam(name = "id", value = "文档ID", required = true, dataType = "long", paramType = "path")
    @Operation(summary = "异步提交图片理解预览")
    public Result<KbImageUnderstandTaskVO> submitPreviewTask(@PathVariable("id") Long id) {
        return Result.success(adminKbDocumentService.submitImageUnderstandPreviewTask(id));
    }

    @PostMapping("/{id}/image-understand/index-async")
    @ApiOperation(value = "异步提交图片理解后一键向量化", notes = "可传前端确认后的图片理解文本，提交后立即返回taskId")
    @ApiImplicitParam(name = "id", value = "文档ID", required = true, dataType = "long", paramType = "path")
    @Operation(summary = "异步提交图片理解后一键向量化")
    public Result<KbImageUnderstandTaskVO> submitImageIndexTask(@PathVariable("id") Long id,
                                                                @RequestBody(required = false) KbImageUnderstandIndexDTO dto) {
        return Result.success(adminKbDocumentService.submitImageUnderstandIndexTask(id, dto));
    }

    @PostMapping("/image-understand/tasks/{taskId}/index-async")
    @ApiOperation(value = "基于任务异步提交图片理解后一键向量化", notes = "指定taskId精准执行向量化任务")
    @ApiImplicitParam(name = "taskId", value = "图片理解任务ID", required = true, dataType = "string", paramType = "path")
    @Operation(summary = "基于任务异步提交图片理解后一键向量化")
    public Result<KbImageUnderstandTaskVO> submitImageIndexTaskByTaskId(@PathVariable("taskId") String taskId,
                                                                        @RequestBody(required = false) KbImageUnderstandIndexDTO dto) {
        return Result.success(adminKbDocumentService.submitImageUnderstandIndexTaskByTaskId(taskId, dto));
    }

    @GetMapping("/image-understand/tasks/{taskId}")
    @ApiOperation(value = "查询图片理解任务状态", notes = "用于前端轮询任务进度与结果")
    @ApiImplicitParam(name = "taskId", value = "任务ID", required = true, dataType = "string", paramType = "path")
    @Operation(summary = "查询图片理解任务状态")
    public Result<KbImageUnderstandTaskVO> getImageTask(@PathVariable("taskId") String taskId) {
        return Result.success(adminKbDocumentService.getImageUnderstandTask(taskId));
    }

    @GetMapping("/image-understand/tasks")
    @ApiOperation(value = "查询图片理解任务列表", notes = "可按documentId过滤，按提交时间倒序")
    @Operation(summary = "查询图片理解任务列表")
    public Result<List<KbImageUnderstandTaskVO>> listImageTasks(@RequestParam(value = "documentId", required = false) Long documentId) {
        return Result.success(adminKbDocumentService.listImageUnderstandTasks(documentId));
    }
}

