package campus.ai.rag.service.service.wx.reminder;

import campus.ai.rag.service.dto.wx.WxScheduleReminderCreateDTO;
import campus.ai.rag.service.vo.wx.WxScheduleNoticePageVO;
import campus.ai.rag.service.vo.wx.WxScheduleReminderVO;

import java.util.List;

public interface WxScheduleReminderService {

    WxScheduleReminderVO create(WxScheduleReminderCreateDTO dto);

    List<WxScheduleReminderVO> listMyReminders();

    void deleteReminder(Long reminderId);

    WxScheduleNoticePageVO listMyNotices();

    void readNotice(Long noticeId);

    void readAllNotices();

    void triggerDueReminders();
}
