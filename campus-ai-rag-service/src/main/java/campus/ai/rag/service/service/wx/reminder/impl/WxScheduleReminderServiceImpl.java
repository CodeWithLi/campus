package campus.ai.rag.service.service.wx.reminder.impl;

import campus.ai.rag.service.common.StatusCodeEnum;
import campus.ai.rag.service.dto.wx.WxScheduleReminderCreateDTO;
import campus.ai.rag.service.entity.forum.ForumScheduleNoticeEntity;
import campus.ai.rag.service.entity.forum.ForumScheduleReminderEntity;
import campus.ai.rag.service.entity.wx.WxUserAuthEntity;
import campus.ai.rag.service.exception.BusinessException;
import campus.ai.rag.service.mapper.forum.ForumScheduleNoticeMapper;
import campus.ai.rag.service.mapper.forum.ForumScheduleReminderMapper;
import campus.ai.rag.service.mapper.wx.WxUserAuthMapper;
import campus.ai.rag.service.properties.WxReminderProperties;
import campus.ai.rag.service.service.wx.reminder.WxScheduleReminderService;
import campus.ai.rag.service.utils.ApiService;
import campus.ai.rag.service.utils.WxAuthContextUtils;
import campus.ai.rag.service.vo.wx.WxScheduleNoticePageVO;
import campus.ai.rag.service.vo.wx.WxScheduleNoticeVO;
import campus.ai.rag.service.vo.wx.WxScheduleReminderVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
public class WxScheduleReminderServiceImpl implements WxScheduleReminderService {

    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int MAX_TITLE_LENGTH = 120;
    private static final String AUTH_TYPE_WX_OPENID = "WX_OPENID";

    private final ForumScheduleReminderMapper forumScheduleReminderMapper;
    private final ForumScheduleNoticeMapper forumScheduleNoticeMapper;
    private final WxUserAuthMapper wxUserAuthMapper;
    private final WxReminderProperties wxReminderProperties;
    private final ApiService apiService;

    public WxScheduleReminderServiceImpl(ForumScheduleReminderMapper forumScheduleReminderMapper,
                                         ForumScheduleNoticeMapper forumScheduleNoticeMapper,
                                         WxUserAuthMapper wxUserAuthMapper,
                                         WxReminderProperties wxReminderProperties,
                                         ApiService apiService) {
        this.forumScheduleReminderMapper = forumScheduleReminderMapper;
        this.forumScheduleNoticeMapper = forumScheduleNoticeMapper;
        this.wxUserAuthMapper = wxUserAuthMapper;
        this.wxReminderProperties = wxReminderProperties;
        this.apiService = apiService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WxScheduleReminderVO create(WxScheduleReminderCreateDTO dto) {
        Long userId = parseUserId(WxAuthContextUtils.requireWxUserId());
        if (dto == null || isBlank(dto.getTitle())) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "提醒标题不能为空");
        }
        String title = dto.getTitle().trim();
        if (title.length() > MAX_TITLE_LENGTH) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "提醒标题长度不能超过" + MAX_TITLE_LENGTH + "字");
        }
        String type = normalizeType(dto.getType());
        LocalDateTime remindAt = parseDateTime(dto.getRemindAt());
        if (remindAt.isBefore(LocalDateTime.now())) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "提醒时间需晚于当前时间");
        }

        ForumScheduleReminderEntity entity = new ForumScheduleReminderEntity();
        entity.setUserId(userId);
        entity.setTitle(title);
        entity.setRemindType(type);
        entity.setRemindAt(remindAt);
        entity.setAdvanceMinutes(resolveAdvanceMinutes());
        entity.setNotified(0);
        entity.setDeleted(0);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        forumScheduleReminderMapper.insert(entity);

        return toReminderVO(entity);
    }

    @Override
    public List<WxScheduleReminderVO> listMyReminders() {
        Long userId = parseUserId(WxAuthContextUtils.requireWxUserId());
        List<ForumScheduleReminderEntity> list = forumScheduleReminderMapper.selectList(new LambdaQueryWrapper<ForumScheduleReminderEntity>()
                .eq(ForumScheduleReminderEntity::getUserId, userId)
                .eq(ForumScheduleReminderEntity::getDeleted, 0)
                .orderByAsc(ForumScheduleReminderEntity::getRemindAt)
                .orderByDesc(ForumScheduleReminderEntity::getId));
        List<WxScheduleReminderVO> out = new ArrayList<>();
        for (ForumScheduleReminderEntity entity : list) {
            out.add(toReminderVO(entity));
        }
        return out;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReminder(Long reminderId) {
        Long userId = parseUserId(WxAuthContextUtils.requireWxUserId());
        if (reminderId == null || reminderId <= 0) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "提醒ID不能为空");
        }
        ForumScheduleReminderEntity entity = forumScheduleReminderMapper.selectById(reminderId);
        if (entity == null || !userId.equals(entity.getUserId()) || Integer.valueOf(1).equals(entity.getDeleted())) {
            throw new BusinessException(StatusCodeEnum.NOT_FOUND, "提醒不存在");
        }
        forumScheduleReminderMapper.update(null, new LambdaUpdateWrapper<ForumScheduleReminderEntity>()
                .eq(ForumScheduleReminderEntity::getId, reminderId)
                .eq(ForumScheduleReminderEntity::getDeleted, 0)
                .set(ForumScheduleReminderEntity::getDeleted, 1)
                .set(ForumScheduleReminderEntity::getUpdatedAt, LocalDateTime.now()));
    }

    @Override
    public WxScheduleNoticePageVO listMyNotices() {
        Long userId = parseUserId(WxAuthContextUtils.requireWxUserId());
        List<ForumScheduleNoticeEntity> list = forumScheduleNoticeMapper.selectList(new LambdaQueryWrapper<ForumScheduleNoticeEntity>()
                .eq(ForumScheduleNoticeEntity::getUserId, userId)
                .orderByDesc(ForumScheduleNoticeEntity::getCreatedAt)
                .orderByDesc(ForumScheduleNoticeEntity::getId)
                .last("limit 200"));
        List<WxScheduleNoticeVO> out = new ArrayList<>();
        int unread = 0;
        for (ForumScheduleNoticeEntity entity : list) {
            WxScheduleNoticeVO vo = toNoticeVO(entity);
            out.add(vo);
            if (Boolean.FALSE.equals(vo.getRead())) {
                unread++;
            }
        }
        WxScheduleNoticePageVO pageVO = new WxScheduleNoticePageVO();
        pageVO.setUnreadCount(unread);
        pageVO.setList(out);
        return pageVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void readNotice(Long noticeId) {
        Long userId = parseUserId(WxAuthContextUtils.requireWxUserId());
        if (noticeId == null || noticeId <= 0) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "通知ID不能为空");
        }
        forumScheduleNoticeMapper.update(null, new LambdaUpdateWrapper<ForumScheduleNoticeEntity>()
                .eq(ForumScheduleNoticeEntity::getId, noticeId)
                .eq(ForumScheduleNoticeEntity::getUserId, userId)
                .set(ForumScheduleNoticeEntity::getIsRead, 1));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void readAllNotices() {
        Long userId = parseUserId(WxAuthContextUtils.requireWxUserId());
        forumScheduleNoticeMapper.update(null, new LambdaUpdateWrapper<ForumScheduleNoticeEntity>()
                .eq(ForumScheduleNoticeEntity::getUserId, userId)
                .eq(ForumScheduleNoticeEntity::getIsRead, 0)
                .set(ForumScheduleNoticeEntity::getIsRead, 1));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Scheduled(cron = "0 */1 * * * ?")
    public void triggerDueReminders() {
        LocalDateTime now = LocalDateTime.now();
        int advanceMinutes = resolveAdvanceMinutes();
        LocalDateTime scanTo = now.plusMinutes(advanceMinutes);
        List<ForumScheduleReminderEntity> dueList = forumScheduleReminderMapper.selectList(new LambdaQueryWrapper<ForumScheduleReminderEntity>()
                .eq(ForumScheduleReminderEntity::getDeleted, 0)
                .eq(ForumScheduleReminderEntity::getNotified, 0)
                .le(ForumScheduleReminderEntity::getRemindAt, scanTo)
                .orderByAsc(ForumScheduleReminderEntity::getRemindAt)
                .last("limit 300"));
        if (dueList == null || dueList.isEmpty()) {
            return;
        }
        for (ForumScheduleReminderEntity reminder : dueList) {
            int updated = forumScheduleReminderMapper.update(null, new LambdaUpdateWrapper<ForumScheduleReminderEntity>()
                    .eq(ForumScheduleReminderEntity::getId, reminder.getId())
                    .eq(ForumScheduleReminderEntity::getDeleted, 0)
                    .eq(ForumScheduleReminderEntity::getNotified, 0)
                    .set(ForumScheduleReminderEntity::getNotified, 1)
                    .set(ForumScheduleReminderEntity::getUpdatedAt, now));
            if (updated <= 0) {
                continue;
            }
            ForumScheduleNoticeEntity notice = new ForumScheduleNoticeEntity();
            notice.setUserId(reminder.getUserId());
            notice.setReminderId(reminder.getId());
            notice.setTitle(reminder.getTitle());
            notice.setRemindType(reminder.getRemindType());
            notice.setContent(buildNoticeContent(reminder, now));
            notice.setIsRead(0);
            notice.setCreatedAt(now);
            forumScheduleNoticeMapper.insert(notice);
            trySendSubscribeMessage(reminder, notice);
        }
    }

    private WxScheduleReminderVO toReminderVO(ForumScheduleReminderEntity entity) {
        WxScheduleReminderVO vo = new WxScheduleReminderVO();
        vo.setId(entity.getId());
        vo.setTitle(entity.getTitle());
        vo.setType(entity.getRemindType());
        vo.setTypeText(typeText(entity.getRemindType()));
        vo.setRemindAt(formatDateTime(entity.getRemindAt()));
        vo.setNotified(Integer.valueOf(1).equals(entity.getNotified()));
        return vo;
    }

    private WxScheduleNoticeVO toNoticeVO(ForumScheduleNoticeEntity entity) {
        WxScheduleNoticeVO vo = new WxScheduleNoticeVO();
        vo.setId(entity.getId());
        vo.setReminderId(entity.getReminderId());
        vo.setTitle(entity.getTitle());
        vo.setType(entity.getRemindType());
        vo.setTypeText(typeText(entity.getRemindType()));
        vo.setContent(entity.getContent());
        vo.setRead(Integer.valueOf(1).equals(entity.getIsRead()));
        vo.setCreatedAt(formatDateTime(entity.getCreatedAt()));
        return vo;
    }

    private static Long parseUserId(String wxUserId) {
        try {
            return Long.parseLong(wxUserId);
        } catch (Exception e) {
            throw new BusinessException(StatusCodeEnum.UNAUTHORIZED, "用户未登录");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String normalizeType(String type) {
        String v = type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
        if ("exam".equals(v) || "ddl".equals(v) || "activity".equals(v)) {
            return v;
        }
        return "activity";
    }

    private static LocalDateTime parseDateTime(Long tsMillis) {
        if (tsMillis == null || tsMillis <= 0) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "提醒时间不能为空");
        }
        try {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(tsMillis), ZoneId.systemDefault());
        } catch (Exception e) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "提醒时间格式错误");
        }
    }

    private static String formatDateTime(LocalDateTime value) {
        if (value == null) return "";
        return DATE_TIME_FMT.format(value);
    }

    private static String typeText(String type) {
        if ("exam".equals(type)) return "考试";
        if ("ddl".equals(type)) return "DDL";
        return "活动";
    }

    private int resolveAdvanceMinutes() {
        int minutes = wxReminderProperties.getAdvanceMinutes() == null ? 30 : wxReminderProperties.getAdvanceMinutes();
        return Math.max(1, minutes);
    }

    private void trySendSubscribeMessage(ForumScheduleReminderEntity reminder, ForumScheduleNoticeEntity notice) {
        WxReminderProperties.Subscribe subscribe = wxReminderProperties.getSubscribe();
        if (subscribe == null || !Boolean.TRUE.equals(subscribe.getEnabled()) || isBlank(subscribe.getTemplateId())) {
            return;
        }
        WxUserAuthEntity auth = wxUserAuthMapper.findByUserIdAndAuthType(reminder.getUserId(), AUTH_TYPE_WX_OPENID);
        if (auth == null || isBlank(auth.getAuthKey())) {
            return;
        }
        Map<String, String> payload = new HashMap<>();
        payload.put(defaultIfBlank(subscribe.getTitleKey(), "thing1"), truncate(notice.getTitle(), 20));
        payload.put(defaultIfBlank(subscribe.getTimeKey(), "time2"), formatDateTime(reminder.getRemindAt()));
        payload.put(defaultIfBlank(subscribe.getTypeKey(), "thing3"), typeText(reminder.getRemindType()));
        apiService.sendMiniAppSubscribeMessage(
                auth.getAuthKey(),
                subscribe.getTemplateId(),
                subscribe.getPage(),
                subscribe.getMiniprogramState(),
                subscribe.getLang(),
                payload
        );
    }

    private static String defaultIfBlank(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private static String buildNoticeContent(ForumScheduleReminderEntity reminder, LocalDateTime now) {
        long minutes = java.time.Duration.between(now, reminder.getRemindAt()).toMinutes();
        String prefix = "你的" + typeText(reminder.getRemindType()) + "提醒";
        if (minutes > 0) {
            return prefix + "还有约" + minutes + "分钟到达时间";
        }
        return prefix + "已到提醒时间";
    }
}
