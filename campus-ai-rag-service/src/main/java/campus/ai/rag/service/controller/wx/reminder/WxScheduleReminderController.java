package campus.ai.rag.service.controller.wx.reminder;

import campus.ai.rag.service.common.Result;
import campus.ai.rag.service.dto.wx.WxScheduleReminderCreateDTO;
import campus.ai.rag.service.service.wx.reminder.WxScheduleReminderService;
import campus.ai.rag.service.vo.wx.WxScheduleNoticePageVO;
import campus.ai.rag.service.vo.wx.WxScheduleReminderVO;
import io.swagger.annotations.Api;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/wx/reminder")
@Api(tags = "微信端日程提醒")
@Tag(name = "微信端日程提醒")
public class WxScheduleReminderController {

    private final WxScheduleReminderService wxScheduleReminderService;

    public WxScheduleReminderController(WxScheduleReminderService wxScheduleReminderService) {
        this.wxScheduleReminderService = wxScheduleReminderService;
    }

    @PostMapping("/create")
    @Operation(summary = "创建日程提醒")
    public Result<WxScheduleReminderVO> create(@RequestBody WxScheduleReminderCreateDTO dto) {
        return Result.success(wxScheduleReminderService.create(dto));
    }

    @GetMapping("/list")
    @Operation(summary = "我的提醒列表")
    public Result<List<WxScheduleReminderVO>> list() {
        return Result.success(wxScheduleReminderService.listMyReminders());
    }

    @PostMapping("/{reminderId}/delete")
    @Operation(summary = "删除提醒")
    public Result<Boolean> delete(@PathVariable("reminderId") Long reminderId) {
        wxScheduleReminderService.deleteReminder(reminderId);
        return Result.success(true);
    }

    @GetMapping("/notice/list")
    @Operation(summary = "通知中心列表")
    public Result<WxScheduleNoticePageVO> noticeList() {
        return Result.success(wxScheduleReminderService.listMyNotices());
    }

    @PostMapping("/notice/{noticeId}/read")
    @Operation(summary = "通知标记已读")
    public Result<Boolean> readNotice(@PathVariable("noticeId") Long noticeId) {
        wxScheduleReminderService.readNotice(noticeId);
        return Result.success(true);
    }

    @PostMapping("/notice/read-all")
    @Operation(summary = "通知全部已读")
    public Result<Boolean> readAll() {
        wxScheduleReminderService.readAllNotices();
        return Result.success(true);
    }
}
