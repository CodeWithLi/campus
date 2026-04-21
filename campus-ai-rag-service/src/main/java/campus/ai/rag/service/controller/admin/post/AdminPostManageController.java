package campus.ai.rag.service.controller.admin.post;

import campus.ai.rag.service.common.Result;
import campus.ai.rag.service.service.admin.post.AdminPostManageService;
import campus.ai.rag.service.vo.admin.post.AdminPostPageVO;
import campus.ai.rag.service.vo.admin.post.AdminPostVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/posts")
@Api(tags = "后台帖子管理")
@Tag(name = "后台帖子管理")
@PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ROLE_SUPER_ADMIN','ROLE_ADMIN')")
public class AdminPostManageController {

    private final AdminPostManageService adminPostManageService;

    public AdminPostManageController(AdminPostManageService adminPostManageService) {
        this.adminPostManageService = adminPostManageService;
    }

    @GetMapping
    @ApiOperation(value = "分页查询帖子", notes = "支持关键词、用户ID、匿名状态、时间范围筛选")
    @Operation(summary = "分页查询帖子")
    public Result<AdminPostPageVO> list(
            @RequestParam(value = "pageNum", defaultValue = "1") Long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") Long pageSize,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "isAnonymous", required = false) Integer isAnonymous,
            @RequestParam(value = "startTime", required = false) String startTime,
            @RequestParam(value = "endTime", required = false) String endTime) {
        return Result.success(adminPostManageService.listPage(
                pageNum, pageSize, keyword, userId, isAnonymous, startTime, endTime
        ));
    }

    @GetMapping("/{postId}")
    @ApiOperation(value = "帖子详情", notes = "按帖子ID查询")
    @Operation(summary = "帖子详情")
    public Result<AdminPostVO> detail(@PathVariable("postId") Long postId) {
        return Result.success(adminPostManageService.detail(postId));
    }

    @DeleteMapping("/{postId}")
    @ApiOperation(value = "删除帖子", notes = "后台删除帖子及关联互动数据")
    @Operation(summary = "删除帖子")
    public Result<Void> delete(@PathVariable("postId") Long postId) {
        adminPostManageService.deletePost(postId);
        return Result.success();
    }
}
