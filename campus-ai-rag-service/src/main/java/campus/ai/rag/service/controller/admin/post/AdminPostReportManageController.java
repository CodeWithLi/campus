package campus.ai.rag.service.controller.admin.post;

import campus.ai.rag.service.common.Result;
import campus.ai.rag.service.dto.admin.post.AdminPostReportHandleDTO;
import campus.ai.rag.service.service.admin.post.AdminPostReportManageService;
import campus.ai.rag.service.vo.admin.post.AdminPostReportPageVO;
import campus.ai.rag.service.vo.admin.post.AdminPostReportVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/post-reports")
@Api(tags = "后台帖子举报管理")
@Tag(name = "后台帖子举报管理")
@PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ROLE_SUPER_ADMIN','ROLE_ADMIN')")
public class AdminPostReportManageController {

    private final AdminPostReportManageService adminPostReportManageService;

    public AdminPostReportManageController(AdminPostReportManageService adminPostReportManageService) {
        this.adminPostReportManageService = adminPostReportManageService;
    }

    @GetMapping
    @ApiOperation(value = "分页查询举报记录", notes = "支持状态、帖子ID、举报用户ID、时间范围筛选")
    @Operation(summary = "分页查询举报记录")
    public Result<AdminPostReportPageVO> list(
            @RequestParam(value = "pageNum", defaultValue = "1") Long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") Long pageSize,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "postId", required = false) Long postId,
            @RequestParam(value = "reporterUserId", required = false) Long reporterUserId,
            @RequestParam(value = "startTime", required = false) String startTime,
            @RequestParam(value = "endTime", required = false) String endTime) {
        return Result.success(adminPostReportManageService.listPage(
                pageNum, pageSize, status, postId, reporterUserId, startTime, endTime
        ));
    }

    @GetMapping("/{reportId}")
    @ApiOperation(value = "举报详情", notes = "按举报ID查询")
    @Operation(summary = "举报详情")
    public Result<AdminPostReportVO> detail(@PathVariable("reportId") Long reportId) {
        return Result.success(adminPostReportManageService.detail(reportId));
    }

    @PutMapping("/{reportId}/handle")
    @ApiOperation(value = "处理举报", notes = "将举报标记为有效/无效，可选删除帖子")
    @Operation(summary = "处理举报")
    public Result<AdminPostReportVO> handle(@PathVariable("reportId") Long reportId,
                                            @RequestBody AdminPostReportHandleDTO dto) {
        return Result.success(adminPostReportManageService.handle(reportId, dto));
    }
}
