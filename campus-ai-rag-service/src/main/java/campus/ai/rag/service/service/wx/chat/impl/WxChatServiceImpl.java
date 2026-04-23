package campus.ai.rag.service.service.wx.chat.impl;

import campus.ai.rag.service.ai.AiChatRouter;
import campus.ai.rag.service.common.StatusCodeEnum;
import campus.ai.rag.service.dto.wx.WxChatDTO;
import campus.ai.rag.service.entity.model.AiModelConfigEntity;
import campus.ai.rag.service.exception.BusinessException;
import campus.ai.rag.service.mapper.model.AiModelConfigMapper;
import campus.ai.rag.service.service.wx.chat.WxChatService;
import campus.ai.rag.service.utils.WxAuthContextUtils;
import campus.ai.rag.service.vo.wx.WxChatVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

@Service
public class WxChatServiceImpl implements WxChatService {

    private final AiChatRouter aiChatRouter;
    private final RedisTemplate<String, Object> redisTemplate;
    private final AiModelConfigMapper aiModelConfigMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.wx.chat.conversation-ttl-days:30}")
    private Integer conversationTtlDays;

    @Value("${app.wx.chat.title-prompt:}")
    private String titlePrompt;

    @Value("${app.wx.chat.summary-prompt:}")
    private String summaryPrompt;

    @Value("${app.wx.chat.max-history-rounds:8}")
    private Integer maxHistoryRounds;

    @Value("${app.wx.chat.summary-compress-every:15}")
    private Integer summaryCompressEvery;

    public WxChatServiceImpl(AiChatRouter aiChatRouter,
                             RedisTemplate<String, Object> redisTemplate,
                             AiModelConfigMapper aiModelConfigMapper) {
        this.aiChatRouter = aiChatRouter;
        this.redisTemplate = redisTemplate;
        this.aiModelConfigMapper = aiModelConfigMapper;
    }

    @Override
    public WxChatVO chat(WxChatDTO dto) {
        return doChat(dto, null, false);
    }

    @Override
    public WxChatVO chatStream(WxChatDTO dto, Consumer<String> onDelta) {
        return doChat(dto, onDelta, true);
    }

    private WxChatVO doChat(WxChatDTO dto, Consumer<String> onDelta, boolean streamMode) {
        if (Objects.isNull(dto)) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "请求参数不能为空");
        }
        boolean hasImage = !isBlank(dto.getImageBase64());
        if (isBlank(dto.getMessage()) && !hasImage) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "消息内容不能为空");
        }

        String userId = WxAuthContextUtils.requireWxUserId();
        Long userIdLong = parseUserId(userId);
        String finalModelCode = isBlank(dto.getModelCode()) ? aiChatRouter.getDefaultModelCode() : dto.getModelCode().trim();
        String convId = isBlank(dto.getConversationId()) ? newConversationId() : dto.getConversationId().trim();

        // 读取摘要与历史
        String summary = getString(metaKey(userId, convId), "summary");
        String historyText = buildHistoryText(historyKey(userId, convId));
        String userMessage = isBlank(dto.getMessage()) ? "请识别图片并回答我关心的重点。" : dto.getMessage().trim();
        String composed = composeChatInput(summary, historyText, userMessage);
        String answer;
        if (hasImage) {
            AiModelConfigEntity model = resolveModel(finalModelCode);
            if (!Objects.equals(model.getSupportsImageChat(), 1)) {
                throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "当前大模型不支持上传图片对话");
            }
            answer = streamMode
                    ? aiChatRouter.streamVisionChat(userIdLong, finalModelCode, composed, dto.getImageBase64(), dto.getImageMimeType(), onDelta)
                    : aiChatRouter.rawVisionChat(userIdLong, finalModelCode, composed, dto.getImageBase64(), dto.getImageMimeType());
        } else {
            answer = streamMode
                    ? aiChatRouter.streamChat(userIdLong, finalModelCode, "CHAT", composed, null, onDelta)
                    : aiChatRouter.chat(userIdLong, finalModelCode, "CHAT", composed, null);
        }

        // 写入历史（user + assistant）
        String storedUserMessage = hasImage ? ("[图片]\n" + userMessage) : userMessage;
        appendHistory(historyKey(userId, convId), "user", storedUserMessage);
        appendHistory(historyKey(userId, convId), "assistant", answer);

        // 首轮生成标题（用独立提示词，不走系统提示词）
        String title = getString(metaKey(userId, convId), "title");
        long msgCount = listSize(historyKey(userId, convId));
        if (isBlank(title) && msgCount <= 2) {
            String firstAnswerShort = truncate(answer, 180);
            String titleInput = titlePrompt + "\n\n【用户首轮问题】\n" + truncate(userMessage, 200)
                    + "\n\n【助手首轮回答】\n" + firstAnswerShort;
            String generated = safeTitle(aiChatRouter.rawChat(finalModelCode, titleInput));
            if (!isBlank(generated)) {
                putString(metaKey(userId, convId), "title", generated);
                title = generated;
            }
        }

        // 满 8 轮：摘要压缩 + 删除这 8 轮
        maybeSummarizeAndTrim(userId, convId, finalModelCode);

        touchTtl(userId, convId);

        WxChatVO vo = new WxChatVO();
        vo.setModelCode(finalModelCode);
        vo.setConversationId(convId);
        vo.setConversationTitle(title);
        vo.setAnswer(answer);
        return vo;
    }

    private void maybeSummarizeAndTrim(String userId, String convId, String modelCode) {
        int rounds = maxHistoryRounds == null ? 8 : Math.max(1, maxHistoryRounds);
        String hKey = historyKey(userId, convId);
        long size = listSize(hKey);
        long threshold = rounds * 2L;
        if (size < threshold) {
            return;
        }
        // 取前 8 轮（16 条消息）做摘要
        @SuppressWarnings("unchecked")
        java.util.List<Object> first = redisTemplate.opsForList().range(hKey, 0, threshold - 1);
        String transcript = toTranscript(first);
        String oldSummary = getString(metaKey(userId, convId), "summary");

        String summaryInput = summaryPrompt + "\n\n【已有摘要】\n" + (oldSummary == null ? "" : oldSummary)
                + "\n\n【新增对话】\n" + transcript;
        String newSummary = aiChatRouter.rawChat(modelCode, summaryInput);
        if (newSummary != null) {
            newSummary = newSummary.trim();
        }
        putString(metaKey(userId, convId), "summary", newSummary);

        // 删除这 8 轮
        redisTemplate.opsForList().trim(hKey, threshold, -1);

        // 摘要累计达到 15 次时再压缩一次（避免摘要无限增长）
        long count = increment(metaKey(userId, convId), "summaryCount");
        int every = summaryCompressEvery == null ? 15 : Math.max(5, summaryCompressEvery);
        if (count > 0 && count % every == 0) {
            String cur = getString(metaKey(userId, convId), "summary");
            if (!isBlank(cur)) {
                String compressInput = summaryPrompt + "\n\n【需要压缩的摘要】\n" + cur;
                String compressed = aiChatRouter.rawChat(modelCode, compressInput);
                putString(metaKey(userId, convId), "summary", compressed == null ? null : compressed.trim());
            }
        }
    }

    private String composeChatInput(String summary, String history, String userMessage) {
        StringBuilder sb = new StringBuilder();
        if (!isBlank(summary)) {
            sb.append("【摘要】\n").append(summary.trim()).append("\n\n");
        }
        if (!isBlank(history)) {
            sb.append("【历史对话】\n").append(history.trim()).append("\n\n");
        }
        sb.append("【用户问题】\n").append(userMessage);
        return sb.toString();
    }

    private String buildHistoryText(String historyKey) {
        @SuppressWarnings("unchecked")
        java.util.List<Object> list = redisTemplate.opsForList().range(historyKey, 0, -1);
        return toTranscript(list);
    }

    private void appendHistory(String historyKey, String role, String content) {
        WxMsg msg = new WxMsg();
        msg.role = role;
        msg.content = content == null ? "" : content;
        redisTemplate.opsForList().rightPush(historyKey, msg);
    }

    private String toTranscript(java.util.List<Object> list) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Object obj : list) {
            if (!(obj instanceof WxMsg)) {
                continue;
            }
            WxMsg m = (WxMsg) obj;
            if (m == null || isBlank(m.content)) {
                continue;
            }
            String who = "user".equalsIgnoreCase(m.role) ? "用户" : "助手";
            sb.append(who).append("：").append(m.content.trim()).append("\n");
        }
        return sb.toString().trim();
    }

    private void touchTtl(String userId, String convId) {
        long days = conversationTtlDays == null ? 30 : Math.max(1, conversationTtlDays);
        Duration ttl = Duration.ofDays(days);
        redisTemplate.expire(historyKey(userId, convId), ttl);
        redisTemplate.expire(metaKey(userId, convId), ttl);
        // 用于会话列表排序展示（前端 history 页需要 updatedAt）
        redisTemplate.opsForHash().put(metaKey(userId, convId), "updatedAt", System.currentTimeMillis());
    }

    private String metaKey(String userId, String convId) {
        return "campus:wx:chat:conv:" + userId + ":" + convId + ":meta";
    }

    private String historyKey(String userId, String convId) {
        return "campus:wx:chat:conv:" + userId + ":" + convId + ":history";
    }

    private String newConversationId() {
        return "c_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
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

    private long increment(String key, String field) {
        Long v = redisTemplate.opsForHash().increment(key, field, 1L);
        return v == null ? 0 : v;
    }

    private String safeTitle(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        // 简单净化：去掉引号/书名号等
        t = t.replace("\"", "").replace("“", "").replace("”", "").replace("《", "").replace("》", "").trim();
        // 限制长度 5~12（允许少量误差）
        if (t.length() > 12) {
            t = t.substring(0, 12).trim();
        }
        return t;
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        if (t.length() <= max) {
            return t;
        }
        return t.substring(0, max) + "…";
    }

    private static class WxMsg {
        public String role;
        public String content;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private Long parseUserId(String wxUserId) {
        try {
            return Long.parseLong(wxUserId);
        } catch (Exception e) {
            throw new BusinessException(StatusCodeEnum.UNAUTHORIZED, "用户未登录");
        }
    }

    private AiModelConfigEntity resolveModel(String modelCode) {
        String targetCode = isBlank(modelCode) ? aiChatRouter.getDefaultModelCode() : modelCode.trim();
        AiModelConfigEntity model = aiModelConfigMapper.selectOne(new LambdaQueryWrapper<AiModelConfigEntity>()
                .eq(AiModelConfigEntity::getEnabled, 1)
                .eq(AiModelConfigEntity::getModelCode, targetCode)
                .last("limit 1"));
        if (model != null) return model;
        String altCode = targetCode.contains("_") ? targetCode.replace("_", " ") : targetCode.replace(" ", "_");
        model = aiModelConfigMapper.selectOne(new LambdaQueryWrapper<AiModelConfigEntity>()
                .eq(AiModelConfigEntity::getEnabled, 1)
                .eq(AiModelConfigEntity::getModelCode, altCode)
                .last("limit 1"));
        if (model == null) {
            throw new BusinessException(StatusCodeEnum.NOT_FOUND, "未找到可用模型配置: " + targetCode);
        }
        return model;
    }
}

