package campus.ai.rag.service.controller.admin.model;

import campus.ai.rag.service.common.Result;
import campus.ai.rag.service.service.admin.model.AdminTokenBillingLogService;
import campus.ai.rag.service.vo.admin.model.AiCallTrendPointVO;
import campus.ai.rag.service.vo.admin.model.AiModelUsageShareVO;
import campus.ai.rag.service.vo.admin.model.AiTokenBillingLogPageVO;
import campus.ai.rag.service.vo.admin.model.AiTokenConsumptionPointVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin")
@Api(tags = "后台 Token 计费日志")
@Tag(name = "后台 Token 计费日志")
@PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ROLE_SUPER_ADMIN','ROLE_ADMIN')")
public class AdminTokenBillingLogController {

    private final AdminTokenBillingLogService adminTokenBillingLogService;

    public AdminTokenBillingLogController(AdminTokenBillingLogService adminTokenBillingLogService) {
        this.adminTokenBillingLogService = adminTokenBillingLogService;
    }

    @GetMapping("/token-billing-logs")
    @ApiOperation(value = "分页查询 Token 消耗日志", notes = "支持用户ID、场景、模型编码、状态、时间范围筛选")
    @Operation(summary = "分页查询 Token 消耗日志")
    public Result<AiTokenBillingLogPageVO> list(
            @RequestParam(value = "pageNum", defaultValue = "1") Long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") Long pageSize,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "scene", required = false) String scene,
            @RequestParam(value = "modelCode", required = false) String modelCode,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "startTime", required = false) String startTime,
            @RequestParam(value = "endTime", required = false) String endTime) {
        return Result.success(adminTokenBillingLogService.listPage(
                pageNum,
                pageSize,
                userId,
                scene,
                modelCode,
                status,
                startTime,
                endTime
        ));
    }

    @GetMapping("/token-billing-logs/stats/trend")
    @ApiOperation(value = "AI 调用趋势", notes = "按天/小时返回调用次数、成功率、平均响应时长")
    @Operation(summary = "AI 调用趋势")
    public Result<List<AiCallTrendPointVO>> callTrend(
            @RequestParam(value = "granularity", defaultValue = "day") String granularity,
            @RequestParam(value = "startTime", required = false) String startTime,
            @RequestParam(value = "endTime", required = false) String endTime) {
        return Result.success(adminTokenBillingLogService.callTrend(granularity, startTime, endTime));
    }

    @GetMapping("/token-billing-logs/stats/consumption")
    @ApiOperation(value = "Token 与积分消耗趋势", notes = "按天/小时返回 token 与积分消耗")
    @Operation(summary = "Token 与积分消耗趋势")
    public Result<List<AiTokenConsumptionPointVO>> tokenConsumption(
            @RequestParam(value = "granularity", defaultValue = "day") String granularity,
            @RequestParam(value = "startTime", required = false) String startTime,
            @RequestParam(value = "endTime", required = false) String endTime) {
        return Result.success(adminTokenBillingLogService.tokenConsumption(granularity, startTime, endTime));
    }

    @GetMapping("/token-billing-logs/stats/model-share")
    @ApiOperation(value = "模型调用占比", notes = "返回各模型调用量与占比")
    @Operation(summary = "模型调用占比")
    public Result<List<AiModelUsageShareVO>> modelUsageShare(
            @RequestParam(value = "startTime", required = false) String startTime,
            @RequestParam(value = "endTime", required = false) String endTime) {
        return Result.success(adminTokenBillingLogService.modelUsageShare(startTime, endTime));
    }
}

