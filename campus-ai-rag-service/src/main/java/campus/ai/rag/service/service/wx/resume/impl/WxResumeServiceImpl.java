package campus.ai.rag.service.service.wx.resume.impl;

import campus.ai.rag.service.ai.AiChatRouter;
import campus.ai.rag.service.common.StatusCodeEnum;
import campus.ai.rag.service.dto.wx.WxResumeChatDTO;
import campus.ai.rag.service.dto.wx.WxResumeGenerateDTO;
import campus.ai.rag.service.entity.model.AiTokenBillingLogEntity;
import campus.ai.rag.service.entity.wx.WxResumeGenerateRecordEntity;
import campus.ai.rag.service.entity.wx.WxUserEntity;
import campus.ai.rag.service.exception.BusinessException;
import campus.ai.rag.service.mapper.model.AiTokenBillingLogMapper;
import campus.ai.rag.service.mapper.wx.WxResumeGenerateRecordMapper;
import campus.ai.rag.service.mapper.wx.WxUserMapper;
import campus.ai.rag.service.service.wx.resume.WxResumeService;
import campus.ai.rag.service.utils.WxAuthContextUtils;
import campus.ai.rag.service.utils.XfResumeHttpUtils;
import campus.ai.rag.service.vo.wx.WxResumeChatVO;
import campus.ai.rag.service.vo.wx.WxResumeGenerateVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class WxResumeServiceImpl implements WxResumeService {

    private static final String SCENE_RESUME = "RESUME";
    private static final double IN_PRICE_PER_1K = 0.01d;
    private static final double OUT_PRICE_PER_1K = 0.02d;
    private static final BigDecimal MIN_CHARGE_POINTS = new BigDecimal("2.00");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final AiChatRouter aiChatRouter;
    private final RedisTemplate<String, Object> redisTemplate;
    private final AiTokenBillingLogMapper aiTokenBillingLogMapper;
    private final WxResumeGenerateRecordMapper wxResumeGenerateRecordMapper;
    private final WxUserMapper wxUserMapper;

    @Value("${app.wx.chat.conversation-ttl-days:30}")
    private Integer conversationTtlDays;

    @Value("${app.wx.chat.title-prompt:}")
    private String titlePrompt;

    @Value("${xf.resume.url:https://cn-huadong-1.xf-yun.com/v1/private/s73f4add9}")
    private String xfResumeUrl;

    @Value("${xf.resume.app-id:}")
    private String xfResumeAppId;

    @Value("${xf.resume.api-key:}")
    private String xfResumeApiKey;

    @Value("${xf.resume.api-secret:}")
    private String xfResumeApiSecret;

    public WxResumeServiceImpl(AiChatRouter aiChatRouter,
                               RedisTemplate<String, Object> redisTemplate,
                               AiTokenBillingLogMapper aiTokenBillingLogMapper,
                               WxResumeGenerateRecordMapper wxResumeGenerateRecordMapper,
                               WxUserMapper wxUserMapper) {
        this.aiChatRouter = aiChatRouter;
        this.redisTemplate = redisTemplate;
        this.aiTokenBillingLogMapper = aiTokenBillingLogMapper;
        this.wxResumeGenerateRecordMapper = wxResumeGenerateRecordMapper;
        this.wxUserMapper = wxUserMapper;
    }

    @Override
    public WxResumeChatVO chat(WxResumeChatDTO dto) {
        if (dto == null || isBlank(dto.getMessage())) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "消息内容不能为空");
        }
        String userId = WxAuthContextUtils.requireWxUserId();
        Long userIdLong = parseUserId(userId);
        String finalModelCode = isBlank(dto.getModelCode()) ? aiChatRouter.getDefaultModelCode() : dto.getModelCode().trim();
        String convId = isBlank(dto.getConversationId()) ? newConversationId() : dto.getConversationId().trim();

        String historyText = buildHistoryText(historyKey(userId, convId));
        String userMessage = dto.getMessage().trim();
        String composed = composeInput(historyText, userMessage);
        String answer = aiChatRouter.chat(userIdLong, finalModelCode, SCENE_RESUME, composed, null);

        appendHistory(historyKey(userId, convId), "user", userMessage);
        appendHistory(historyKey(userId, convId), "assistant", answer);

        String title = getString(metaKey(userId, convId), "title");
        long msgCount = listSize(historyKey(userId, convId));
        if (isBlank(title) && msgCount <= 2 && !isBlank(titlePrompt)) {
            String titleInput = titlePrompt + "\n\n【用户首轮问题】\n" + truncate(userMessage, 200)
                    + "\n\n【助手首轮回答】\n" + truncate(answer, 180);
            String generated = safeTitle(aiChatRouter.rawChat(userIdLong, finalModelCode, titleInput));
            if (!isBlank(generated)) {
                putString(metaKey(userId, convId), "title", generated);
                title = generated;
            }
        }
        touchTtl(userId, convId);

        WxResumeChatVO vo = new WxResumeChatVO();
        vo.setModelCode(finalModelCode);
        vo.setConversationId(convId);
        vo.setConversationTitle(title);
        vo.setAnswer(answer);
        return vo;
    }

    @Override
    public WxResumeGenerateVO generate(WxResumeGenerateDTO dto) {
        if (dto == null || isBlank(dto.getResumeText())) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "resumeText不能为空");
        }
        Long userId = parseUserId(WxAuthContextUtils.requireWxUserId());
        ensureMinimumChargeBalance(userId);

        // 第二步是调用“简历生成API”，不拼系统提示词；只提交用户最终确认的简历信息。
        String reqText = buildResumeGenerateInput(dto);
        try {
            XfResumeHttpUtils.ResumeResult result = XfResumeHttpUtils.generate(
                    xfResumeUrl,
                    xfResumeAppId,
                    xfResumeApiKey,
                    xfResumeApiSecret,
                    reqText
            );

            WxResumeGenerateVO vo = new WxResumeGenerateVO();
            vo.setSid(result.sid);
            vo.setLinks(mapLinks(result.links));
            saveResumeGenerateRecord(userId, dto, vo);

            int promptTokens = estimateTokensByChars(reqText.length());
            int completionTokens = estimateTokensByChars(linkText(vo).length());
            BigDecimal consumedPoints = calcConsumedPoints(promptTokens, completionTokens);
            doDeductPowerPoints(userId, consumedPoints);
            recordResumeBilling(userId, promptTokens, completionTokens, consumedPoints, null);
            return vo;
        } catch (RuntimeException ex) {
            recordResumeBilling(userId, estimateTokensByChars(reqText.length()), 0, BigDecimal.ZERO, ex.getMessage());
            throw ex;
        }
    }

    private String buildResumeGenerateInput(WxResumeGenerateDTO dto) {
        String templateType = normalizeTemplateType(dto.getTemplateType());
        String jobDirection = isBlank(dto.getJobDirection()) ? "" : dto.getJobDirection().trim();
        String resumeText = dto.getResumeText().trim();

        String templateLabel = switch (templateType) {
            case "EXPERIENCED" -> "社招";
            case "INTERN" -> "实习";
            default -> "校招";
        };

        StringBuilder sb = new StringBuilder();
        sb.append("【简历生成要求】\n");
        sb.append("- 模板类型：").append(templateLabel).append("\n");
        if (!isBlank(jobDirection)) {
            sb.append("- 岗位方向：").append(jobDirection).append("\n");
        }
        sb.append("- 输出：请生成专业简历内容，并尽量给出1~3份结果链接（若服务支持）\n\n");
        sb.append("【简历信息】\n").append(resumeText);
        return sb.toString();
    }

    private String normalizeTemplateType(String templateType) {
        if (isBlank(templateType)) return "CAMPUS";
        String t = templateType.trim().toUpperCase();
        return switch (t) {
            case "CAMPUS", "EXPERIENCED", "INTERN" -> t;
            default -> "CAMPUS";
        };
    }

    private List<WxResumeGenerateVO.LinkVO> mapLinks(List<Map<String, Object>> links) {
        if (links == null || links.isEmpty()) return List.of();
        return links.stream().map(m -> {
            WxResumeGenerateVO.LinkVO vo = new WxResumeGenerateVO.LinkVO();
            vo.setImgUrl(m == null ? null : toStr(m.get("img_url")));
            vo.setWordUrl(m == null ? null : toStr(m.get("word_url")));
            return vo;
        }).toList();
    }

    private void saveResumeGenerateRecord(Long userId, WxResumeGenerateDTO dto, WxResumeGenerateVO vo) {
        WxResumeGenerateRecordEntity record = new WxResumeGenerateRecordEntity();
        record.setUserId(userId);
        record.setSid(vo == null ? null : vo.getSid());
        record.setTemplateType(normalizeTemplateType(dto == null ? null : dto.getTemplateType()));
        record.setJobDirection(dto == null ? null : truncate(dto.getJobDirection(), 120));
        record.setResumeText(dto == null ? null : dto.getResumeText());
        record.setLinksJson(toJsonString(vo == null ? null : vo.getLinks()));
        record.setLinkCount(vo == null || vo.getLinks() == null ? 0 : vo.getLinks().size());
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        wxResumeGenerateRecordMapper.insert(record);
    }

    private void recordResumeBilling(Long userId, int promptTokens, int completionTokens, BigDecimal consumedPoints, String errMsg) {
        AiTokenBillingLogEntity log = new AiTokenBillingLogEntity();
        log.setUserId(userId);
        log.setScene(SCENE_RESUME);
        log.setModelCode("xf_resume_api");
        log.setInvokeType(2);
        log.setBillingType("ESTIMATE_CHAR");
        log.setPromptTokens(Math.max(0, promptTokens));
        log.setCompletionTokens(Math.max(0, completionTokens));
        log.setTotalTokens(Math.max(0, promptTokens + completionTokens));
        log.setRequestChars(0);
        log.setResponseChars(0);
        log.setConsumedPoints(consumedPoints == null ? BigDecimal.ZERO : consumedPoints.max(BigDecimal.ZERO));
        log.setStatus(errMsg == null ? 1 : 0);
        log.setErrorMessage(errMsg == null ? null : truncate(errMsg, 1000));
        aiTokenBillingLogMapper.insert(log);
    }

    private String composeInput(String history, String userMessage) {
        StringBuilder sb = new StringBuilder();
        if (!isBlank(history)) {
            sb.append("【历史简历对话】\n").append(history.trim()).append("\n\n");
        }
        sb.append("【用户问题】\n").append(userMessage);
        return sb.toString();
    }

    private String buildHistoryText(String historyKey) {
        @SuppressWarnings("unchecked")
        List<Object> list = redisTemplate.opsForList().range(historyKey, 0, -1);
        if (list == null || list.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Object obj : list) {
            if (!(obj instanceof WxMsg)) continue;
            WxMsg m = (WxMsg) obj;
            if (m == null || isBlank(m.content)) continue;
            String who = "user".equalsIgnoreCase(m.role) ? "用户" : "助手";
            sb.append(who).append("：").append(m.content.trim()).append("\n");
        }
        return sb.toString().trim();
    }

    private void appendHistory(String historyKey, String role, String content) {
        WxMsg msg = new WxMsg();
        msg.role = role;
        msg.content = content == null ? "" : content;
        redisTemplate.opsForList().rightPush(historyKey, msg);
    }

    private void touchTtl(String userId, String convId) {
        long days = conversationTtlDays == null ? 30 : Math.max(1, conversationTtlDays);
        Duration ttl = Duration.ofDays(days);
        redisTemplate.expire(historyKey(userId, convId), ttl);
        redisTemplate.expire(metaKey(userId, convId), ttl);
        redisTemplate.opsForHash().put(metaKey(userId, convId), "updatedAt", System.currentTimeMillis());
    }

    private String metaKey(String userId, String convId) {
        return "campus:wx:resume:conv:" + userId + ":" + convId + ":meta";
    }

    private String historyKey(String userId, String convId) {
        return "campus:wx:resume:conv:" + userId + ":" + convId + ":history";
    }

    private String newConversationId() {
        return "rsm_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private long listSize(String key) {
        Long size = redisTemplate.opsForList().size(key);
        return size == null ? 0 : size;
    }

    private String getString(String key, String field) {
        Object v = redisTemplate.opsForHash().get(key, field);
        return v == null ? null : String.valueOf(v);
    }

    private void putString(String key, String field, String value) {
        if (value == null) {
            redisTemplate.opsForHash().delete(key, field);
        } else {
            redisTemplate.opsForHash().put(key, field, value);
        }
    }

    private String safeTitle(String raw) {
        if (raw == null) return null;
        String t = raw.trim().replace("\"", "").replace("“", "").replace("”", "").replace("《", "").replace("》", "").trim();
        if (t.length() > 12) {
            t = t.substring(0, 12).trim();
        }
        return t;
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        String t = s.trim();
        return t.length() <= max ? t : t.substring(0, max) + "…";
    }

    private int estimateTokensByChars(int chars) {
        if (chars <= 0) return 0;
        return (int) Math.ceil(chars / 1.5d);
    }

    private BigDecimal calcConsumedPoints(int promptTokens, int completionTokens) {
        int safePrompt = Math.max(0, promptTokens);
        int safeCompletion = Math.max(0, completionTokens);

        BigDecimal inCost = BigDecimal.valueOf(safePrompt)
                .divide(new BigDecimal("1000"), 8, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(IN_PRICE_PER_1K));
        BigDecimal outCost = BigDecimal.valueOf(safeCompletion)
                .divide(new BigDecimal("1000"), 8, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(OUT_PRICE_PER_1K));
        BigDecimal cost = inCost.add(outCost);
        if (cost.compareTo(MIN_CHARGE_POINTS) < 0) {
            cost = MIN_CHARGE_POINTS;
        }
        return cost.setScale(2, RoundingMode.CEILING);
    }

    private void ensureMinimumChargeBalance(Long userId) {
        if (userId == null || userId <= 0L) return;
        WxUserEntity user = wxUserMapper.selectById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "用户不存在或已被禁用");
        }
        BigDecimal balance = user.getPowerPoints() == null ? BigDecimal.ZERO : user.getPowerPoints();
        if (balance.compareTo(MIN_CHARGE_POINTS) < 0) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "算力点不足，请先充值");
        }
    }

    private void doDeductPowerPoints(Long userId, BigDecimal points) {
        if (userId == null || userId <= 0L || points == null || points.compareTo(BigDecimal.ZERO) <= 0) return;
        BigDecimal p = points.setScale(2, RoundingMode.CEILING);
        int affected = wxUserMapper.deductPowerPoints(userId, p);
        if (affected <= 0) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "算力点不足，请先充值");
        }
    }

    private Long parseUserId(String userId) {
        if (isBlank(userId)) return -1L;
        try {
            return Long.parseLong(userId.trim());
        } catch (Exception ex) {
            return -1L;
        }
    }

    private String linkText(WxResumeGenerateVO vo) {
        if (vo == null || vo.getLinks() == null || vo.getLinks().isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (WxResumeGenerateVO.LinkVO l : vo.getLinks()) {
            if (l == null) continue;
            if (!isBlank(l.getWordUrl())) sb.append(l.getWordUrl());
            if (!isBlank(l.getImgUrl())) sb.append(l.getImgUrl());
        }
        return sb.toString();
    }

    private String toStr(Object obj) {
        return obj == null ? null : String.valueOf(obj);
    }

    private String toJsonString(Object obj) {
        if (obj == null) return null;
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (Exception ex) {
            return String.valueOf(obj);
        }
    }

    private boolean isBlank(String v) {
        return v == null || v.trim().isEmpty();
    }

    private static class WxMsg {
        public String role;
        public String content;
    }
}

