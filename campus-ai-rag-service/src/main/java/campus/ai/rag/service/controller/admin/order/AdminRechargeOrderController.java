package campus.ai.rag.service.controller.admin.order;

import campus.ai.rag.service.common.Result;
import campus.ai.rag.service.service.admin.order.AdminRechargeOrderService;
import campus.ai.rag.service.vo.admin.order.AdminRechargeOrderPageVO;
import campus.ai.rag.service.vo.admin.order.AdminRechargeOrderVO;
import io.swagger.annotations.Api;
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
@RequestMapping("/admin/recharge-orders")
@Api(tags = "后台充值订单管理")
@Tag(name = "后台充值订单管理")
@PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ROLE_SUPER_ADMIN','ROLE_ADMIN')")
public class AdminRechargeOrderController {

    private final AdminRechargeOrderService adminRechargeOrderService;

    public AdminRechargeOrderController(AdminRechargeOrderService adminRechargeOrderService) {
        this.adminRechargeOrderService = adminRechargeOrderService;
    }

    @GetMapping
    @ApiOperation(value = "分页查询充值订单", notes = "支持订单号、用户ID、状态、渠道、时间范围筛选")
    @Operation(summary = "分页查询充值订单")
    public Result<AdminRechargeOrderPageVO> list(
            @RequestParam(value = "pageNum", defaultValue = "1") Long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") Long pageSize,
            @RequestParam(value = "orderNo", required = false) String orderNo,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "channel", required = false) String channel,
            @RequestParam(value = "startTime", required = false) String startTime,
            @RequestParam(value = "endTime", required = false) String endTime) {
        return Result.success(adminRechargeOrderService.listPage(
                pageNum, pageSize, orderNo, userId, status, channel, startTime, endTime
        ));
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "查询订单详情", notes = "按订单主键ID查询")
    @Operation(summary = "查询订单详情")
    public Result<AdminRechargeOrderVO> detail(@PathVariable("id") Long id) {
        return Result.success(adminRechargeOrderService.getById(id));
    }
}
