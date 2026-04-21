package campus.ai.rag.service.controller.wx.reminder;

import campus.ai.rag.service.common.Result;
import campus.ai.rag.service.dto.wx.WxNoticeAgentCreateDTO;
import campus.ai.rag.service.dto.wx.WxScheduleReminderCreateDTO;
import campus.ai.rag.service.service.wx.reminder.WxNoticeAgentService;
import campus.ai.rag.service.service.wx.reminder.WxScheduleReminderService;
import campus.ai.rag.service.vo.wx.WxNoticeAgentParseVO;
import campus.ai.rag.service.vo.wx.WxNoticeAgentRecordVO;
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
    private final WxNoticeAgentService wxNoticeAgentService;

    public WxScheduleReminderController(WxScheduleReminderService wxScheduleReminderService,
                                        WxNoticeAgentService wxNoticeAgentService) {
        this.wxScheduleReminderService = wxScheduleReminderService;
        this.wxNoticeAgentService = wxNoticeAgentService;
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

    @PostMapping("/agent/parse")
    @Operation(summary = "通知智能体解析（图片/PDF）")
    public Result<WxNoticeAgentParseVO> parseNotice(@RequestPart("file") org.springframework.web.multipart.MultipartFile file) {
        return Result.success(wxNoticeAgentService.parse(file));
    }

    @PostMapping("/agent/parse-image-understand")
    @Operation(summary = "通知智能体解析（图片理解）")
    public Result<WxNoticeAgentParseVO> parseNoticeByImageUnderstand(@RequestPart("file") org.springframework.web.multipart.MultipartFile file) {
        return Result.success(wxNoticeAgentService.parseByImageUnderstand(file));
    }

    @PostMapping("/agent/parse-image-model")
    @Operation(summary = "通知智能体解析（图片直传大模型）")
    public Result<WxNoticeAgentParseVO> parseNoticeByImageModel(@RequestPart("file") org.springframework.web.multipart.MultipartFile file) {
        return Result.success(wxNoticeAgentService.parseByImageModel(file));
    }

    @PostMapping("/agent/parse-image-vision")
    @Operation(summary = "通知智能体解析（图像与视频理解-SpringAI）")
    public Result<WxNoticeAgentParseVO> parseNoticeByImageVision(@RequestPart("file") org.springframework.web.multipart.MultipartFile file) {
        return Result.success(wxNoticeAgentService.parseByImageVision(file));
    }

    @PostMapping("/agent/create")
    @Operation(summary = "通知智能体批量创建提醒")
    public Result<List<WxScheduleReminderVO>> createByAgent(@RequestBody WxNoticeAgentCreateDTO dto) {
        return Result.success(wxNoticeAgentService.createBatch(dto));
    }

    @GetMapping("/agent/records")
    @Operation(summary = "通知智能体解析历史")
    public Result<List<WxNoticeAgentRecordVO>> records() {
        return Result.success(wxNoticeAgentService.listRecords());
    }

    @PostMapping("/agent/records/{recordId}/delete")
    @Operation(summary = "删除通知智能体解析记录")
    public Result<Boolean> deleteRecord(@PathVariable("recordId") Long recordId) {
        wxNoticeAgentService.deleteRecord(recordId);
        return Result.success(true);
    }
}
