package campus.ai.rag.service.ai;

import campus.ai.rag.service.common.StatusCodeEnum;
import campus.ai.rag.service.entity.model.AiModelConfigEntity;
import campus.ai.rag.service.entity.model.AiTokenBillingLogEntity;
import campus.ai.rag.service.entity.wx.WxUserEntity;
import campus.ai.rag.service.exception.BusinessException;
import campus.ai.rag.service.mapper.model.AiModelConfigMapper;
import campus.ai.rag.service.mapper.model.AiTokenBillingLogMapper;
import campus.ai.rag.service.mapper.wx.WxUserMapper;
import campus.ai.rag.service.service.prompt.SystemPromptRuntimeService;
import campus.ai.rag.service.utils.GenericHttpUtils;
import campus.ai.rag.service.utils.XfHttpUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 按 modelCode 路由到对应 ChatClient。后续做“后台可配置”时，只需要把配置落库并刷新 registry 即可。
 */
public class AiChatRouter {

    private static final int INVOKE_TYPE_SPRING_AI = 1;
    private static final int INVOKE_TYPE_XF_HTTP = 2;
    private static final int INVOKE_TYPE_GENERIC_HTTP = 3;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final double IN_PRICE_PER_1K = 0.01d;
    private static final double OUT_PRICE_PER_1K = 0.02d;
    private static final BigDecimal MIN_CHARGE_POINTS = new BigDecimal("2.00");
    private static final BigDecimal BILLING_TOKEN_MULTIPLIER = new BigDecimal("1.5");
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    /**
     * 微信端 AI 对话免费场景（不扣算力点）。
     * 说明：仅影响扣费与“算力点不足”的拦截，不影响计费日志记录（consumedPoints=0）。
     */
    private static final Set<String> FREE_SCENES = Set.of("CHAT", "RAG", "RESUME");

    private final String defaultModelCode;
    private final AiModelConfigMapper aiModelConfigMapper;
    private final AiTokenBillingLogMapper aiTokenBillingLogMapper;
    private final WxUserMapper wxUserMapper;
    private final SystemPromptRuntimeService systemPromptRuntimeService;

    public AiChatRouter(String defaultModelCode,
                        AiModelConfigMapper aiModelConfigMapper,
                        AiTokenBillingLogMapper aiTokenBillingLogMapper,
                        WxUserMapper wxUserMapper,
                        SystemPromptRuntimeService systemPromptRuntimeService) {
        this.defaultModelCode = defaultModelCode;
        this.aiModelConfigMapper = aiModelConfigMapper;
        this.aiTokenBillingLogMapper = aiTokenBillingLogMapper;
        this.wxUserMapper = wxUserMapper;
        this.systemPromptRuntimeService = systemPromptRuntimeService;
    }

    public String chat(String modelCode, String userMessage) {
        return chat(-1L, modelCode, "CHAT", userMessage, null);
    }

    public String chat(String modelCode, String scene, String userMessage, String ragContext) {
        return chat(-1L, modelCode, scene, userMessage, ragContext);
    }

    public String chat(Long userId, String modelCode, String scene, String userMessage, String ragContext) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "消息内容不能为空");
        }
        String promptedMessage = systemPromptRuntimeService.buildPromptedMessage(scene, userMessage, ragContext);
        AiModelConfigEntity modelConfig = resolveModel(modelCode);
        ensureMinimumChargeBalance(safeUserId(userId), safeScene(scene));
        long startMs = System.currentTimeMillis();
        try {
            ChatInvokeResult result = invokeByType(modelConfig, promptedMessage);
            recordBilling(safeUserId(userId), safeScene(scene), modelConfig, promptedMessage, result, null, System.currentTimeMillis() - startMs);
            return result.answer;
        } catch (RuntimeException ex) {
            recordBillingFailure(safeUserId(userId), safeScene(scene), modelConfig, promptedMessage, ex.getMessage(), System.currentTimeMillis() - startMs);
            throw ex;
        }
    }

    /**
     * 原样调用大模型（不拼接系统提示词），用于生成标题/摘要等“内部任务”。
     */
    public String rawChat(String modelCode, String userMessage) {
        return rawChat(-1L, modelCode, userMessage);
    }

    public String rawChat(Long userId, String modelCode, String userMessage) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "消息内容不能为空");
        }
        AiModelConfigEntity modelConfig = resolveModel(modelCode);
        long startMs = System.currentTimeMillis();
        try {
            ChatInvokeResult result = invokeByType(modelConfig, userMessage);
            recordBilling(safeUserId(userId), "INTERNAL", modelConfig, userMessage, result, null, System.currentTimeMillis() - startMs);
            return result.answer;
        } catch (RuntimeException ex) {
            recordBillingFailure(safeUserId(userId), "INTERNAL", modelConfig, userMessage, ex.getMessage(), System.currentTimeMillis() - startMs);
            throw ex;
        }
    }

    public String getDefaultModelCode() {
        return defaultModelCode;
    }

    public String rawVisionChat(Long userId,
                                String modelCode,
                                String userMessage,
                                String imageBase64,
                                String imageMimeType) {
        if (imageBase64 == null || imageBase64.trim().isEmpty()) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "图片内容不能为空");
        }
        String prompt = (userMessage == null || userMessage.trim().isEmpty())
                ? "请先识别图片内容，再回答用户问题。"
                : userMessage.trim();
        AiModelConfigEntity modelConfig = resolveModel(modelCode);
        if (!Objects.equals(modelConfig.getSupportsImageChat(), 1)) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "当前模型不支持图片对话");
        }
        long startMs = System.currentTimeMillis();
        try {
            ChatInvokeResult result = visionHttpChat(modelConfig, prompt, imageBase64.trim(), imageMimeType);
            recordBilling(safeUserId(userId), "INTERNAL", modelConfig, prompt, result, null, System.currentTimeMillis() - startMs);
            return result.answer;
        } catch (RuntimeException ex) {
            recordBillingFailure(safeUserId(userId), "INTERNAL", modelConfig, prompt, ex.getMessage(), System.currentTimeMillis() - startMs);
            throw ex;
        }
    }

    public void validate() {
        Objects.requireNonNull(defaultModelCode, "defaultModelCode 不能为空");
        Objects.requireNonNull(aiModelConfigMapper, "aiModelConfigMapper 不能为空");
        Objects.requireNonNull(aiTokenBillingLogMapper, "aiTokenBillingLogMapper 不能为空");
        Objects.requireNonNull(wxUserMapper, "wxUserMapper 不能为空");
        Objects.requireNonNull(systemPromptRuntimeService, "systemPromptRuntimeService 不能为空");
    }

    private ChatInvokeResult invokeByType(AiModelConfigEntity modelConfig, String userMessage) {
        if (Objects.equals(modelConfig.getInvokeType(), INVOKE_TYPE_SPRING_AI)) {
            return springAiChat(modelConfig, userMessage);
        }
        if (Objects.equals(modelConfig.getInvokeType(), INVOKE_TYPE_XF_HTTP)) {
            return xfHttpChat(modelConfig, userMessage);
        }
        if (Objects.equals(modelConfig.getInvokeType(), INVOKE_TYPE_GENERIC_HTTP)) {
            return genericHttpChat(modelConfig, userMessage);
        }
        throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "不支持的调用方式: " + modelConfig.getInvokeType());
    }

    private ChatInvokeResult springAiChat(AiModelConfigEntity modelConfig, String userMessage) {
        if (isBlank(modelConfig.getApiBaseUrl())) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "springai模式下 api_base_url 不能为空");
        }
        if (isBlank(modelConfig.getApiKey())) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "springai模式下 api_key 不能为空");
        }
        if (modelConfig.getModelNameRemote() == null || modelConfig.getModelNameRemote().trim().isEmpty()) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "springai模式下 model_name_remote 不能为空");
        }

        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(modelConfig.getApiBaseUrl())
                .apiKey(modelConfig.getApiKey())
                .build();

        OpenAiChatModel openAiChatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .build();

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(modelConfig.getModelNameRemote())
                .temperature(modelConfig.getTemperature())
                .maxTokens(modelConfig.getMaxTokens())
                .build();

        Prompt prompt = new Prompt(List.of(new UserMessage(userMessage)), options);
        Object response = openAiChatModel.call(prompt);
        String text = "";
        Integer promptTokens = null;
        Integer completionTokens = null;
        Integer totalTokens = null;
        String extJson = null;
        try {
            Object result = response.getClass().getMethod("getResult").invoke(response);
            Object output = result.getClass().getMethod("getOutput").invoke(result);
            Object textObj = output.getClass().getMethod("getText").invoke(output);
            text = textObj == null ? "" : String.valueOf(textObj);
        } catch (Exception ignore) {
            text = "";
        }
        try {
            Object metadata = response.getClass().getMethod("getMetadata").invoke(response);
            if (metadata != null) {
                Object usage = metadata.getClass().getMethod("getUsage").invoke(metadata);
                if (usage != null) {
                    promptTokens = toIntSafe(invokeGetter(usage, "getPromptTokens"));
                    completionTokens = toIntSafe(invokeGetter(usage, "getCompletionTokens"));
                    totalTokens = toIntSafe(invokeGetter(usage, "getTotalTokens"));
                    extJson = String.valueOf(usage);
                }
            }
        } catch (Exception ignore) {
            // 保留空 usage，后续走估算
        }
        return ChatInvokeResult.of(text, promptTokens, completionTokens, totalTokens, "TOKEN_API", extJson);
    }

    private ChatInvokeResult xfHttpChat(AiModelConfigEntity modelConfig, String userMessage) {
        if (isBlank(modelConfig.getApiBaseUrl())) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "xf_http模式下 api_base_url 不能为空");
        }
        if (isBlank(modelConfig.getApiKey())) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "xf_http模式下 api_key 不能为空");
        }
        if (isBlank(modelConfig.getModelNameRemote())) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "xf_http模式下 model_name_remote 不能为空");
        }

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", userMessage);
        List<Map<String, Object>> messages = List.of(message);

        String response = XfHttpUtils.chatCompletions(
                modelConfig.getApiBaseUrl(),
                modelConfig.getApiKey(),
                modelConfig.getModelNameRemote(),
                "admin-user",
                messages
        );
        String content = XfHttpUtils.extractContent(response);
        Integer promptTokens = null;
        Integer completionTokens = null;
        Integer totalTokens = null;
        try {
            Map<String, Object> root = OBJECT_MAPPER.readValue(response, new TypeReference<>() {
            });
            Object usageObj = root.get("usage");
            if (usageObj instanceof Map) {
                Map<?, ?> usage = (Map<?, ?>) usageObj;
                Integer p = toIntSafe(usage.get("prompt_tokens"));
                Integer sp = toIntSafe(usage.get("search_prompt_tokens"));
                Integer c = toIntSafe(usage.get("completion_tokens"));
                Integer t = toIntSafe(usage.get("total_tokens"));
                promptTokens = (p == null ? 0 : p) + (sp == null ? 0 : sp);
                completionTokens = c;
                totalTokens = t;
            }
        } catch (Exception ignore) {
            // usage 解析失败则走估算
        }
        return ChatInvokeResult.of(content, promptTokens, completionTokens, totalTokens, "TOKEN_API", response);
    }

    private ChatInvokeResult genericHttpChat(AiModelConfigEntity modelConfig, String userMessage) {
        if (isBlank(modelConfig.getApiBaseUrl())) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "generic_http模式下 api_base_url 不能为空");
        }
        if (isBlank(modelConfig.getGenericReqType())) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "generic_http模式下 generic_req_type 不能为空");
        }
        if (isBlank(modelConfig.getGenericReqField())) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "generic_http模式下 generic_req_field 不能为空");
        }

        GenericHttpUtils.ReqType reqType;
        try {
            reqType = GenericHttpUtils.ReqType.valueOf(modelConfig.getGenericReqType().trim().toUpperCase());
        } catch (Exception e) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "generic_req_type 仅支持 messages/prompt/input");
        }

        String content = GenericHttpUtils.postChat(
                modelConfig.getApiBaseUrl(),
                modelConfig.getGenericHeadersJson(),
                reqType,
                modelConfig.getGenericReqField(),
                userMessage,
                modelConfig.getGenericRespPath()
        );
        return ChatInvokeResult.of(content, null, null, null, "ESTIMATE_CHAR", null);
    }

    private ChatInvokeResult visionHttpChat(AiModelConfigEntity modelConfig,
                                            String userMessage,
                                            String imageBase64,
                                            String imageMimeType) {
        String endpoint = buildChatCompletionsEndpoint(modelConfig.getApiBaseUrl());
        String mime = (imageMimeType == null || imageMimeType.trim().isEmpty()) ? "image/jpeg" : imageMimeType.trim();
        String dataUrl = "data:" + mime + ";base64," + imageBase64;
        Map<String, Object> body = new HashMap<>();
        body.put("model", modelConfig.getModelNameRemote());
        body.put("temperature", modelConfig.getTemperature());
        body.put("max_tokens", modelConfig.getMaxTokens());
        body.put("messages", List.of(Map.of(
                "role", "user",
                "content", List.of(
                        Map.of("type", "text", "text", userMessage),
                        Map.of("type", "image_url", "image_url", Map.of("url", dataUrl))
                )
        )));
        try {
            String bodyJson = OBJECT_MAPPER.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(120))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + modelConfig.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(bodyJson, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR,
                        "视觉模型调用失败，status=" + response.statusCode() + "，body=" + truncate(response.body(), 300));
            }
            String responseText = response.body();
            Map<String, Object> root = OBJECT_MAPPER.readValue(responseText, new TypeReference<>() {
            });
            String content = extractVisionContent(root);
            Integer promptTokens = null;
            Integer completionTokens = null;
            Integer totalTokens = null;
            Object usageObj = root.get("usage");
            if (usageObj instanceof Map<?, ?> usage) {
                promptTokens = toIntSafe(usage.get("prompt_tokens"));
                completionTokens = toIntSafe(usage.get("completion_tokens"));
                totalTokens = toIntSafe(usage.get("total_tokens"));
            }
            return ChatInvokeResult.of(content, promptTokens, completionTokens, totalTokens, "TOKEN_API", responseText);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "视觉模型调用失败：" + e.getMessage());
        }
    }

    private String extractVisionContent(Map<String, Object> root) {
        Object choicesObj = root.get("choices");
        if (!(choicesObj instanceof List<?> choices) || choices.isEmpty()) {
            return "";
        }
        Object firstObj = choices.get(0);
        if (!(firstObj instanceof Map<?, ?> first)) {
            return "";
        }
        Object messageObj = first.get("message");
        if (!(messageObj instanceof Map<?, ?> message)) {
            return "";
        }
        Object contentObj = message.get("content");
        if (contentObj instanceof String s) {
            return s;
        }
        if (contentObj instanceof List<?> parts) {
            StringBuilder sb = new StringBuilder();
            for (Object partObj : parts) {
                if (partObj instanceof Map<?, ?> part) {
                    Object textObj = part.get("text");
                    if (textObj != null) {
                        sb.append(String.valueOf(textObj)).append('\n');
                    }
                }
            }
            return sb.toString().trim();
        }
        return contentObj == null ? "" : String.valueOf(contentObj);
    }

    private String buildChatCompletionsEndpoint(String apiBaseUrl) {
        if (isBlank(apiBaseUrl)) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "springai模式下 api_base_url 不能为空");
        }
        String base = apiBaseUrl.trim();
        if (base.endsWith("/chat/completions")) {
            return base;
        }
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (!base.endsWith("/v1")) {
            base = base + "/v1";
        }
        return base + "/chat/completions";
    }

    private AiModelConfigEntity resolveModel(String modelCode) {
        String targetCode = (modelCode == null || modelCode.trim().isEmpty()) ? defaultModelCode : modelCode.trim();
        AiModelConfigEntity model = aiModelConfigMapper.selectOne(new LambdaQueryWrapper<AiModelConfigEntity>()
                .eq(AiModelConfigEntity::getEnabled, 1)
                .eq(AiModelConfigEntity::getModelCode, targetCode)
                .last("limit 1"));
        if (Objects.nonNull(model)) {
            return model;
        }
        // 兼容 "qwen_plus" 与 "qwen plus" 这类编码差异
        String altCode = targetCode.contains("_")
                ? targetCode.replace("_", " ")
                : targetCode.replace(" ", "_");
        if (!Objects.equals(altCode, targetCode)) {
            AiModelConfigEntity alt = aiModelConfigMapper.selectOne(new LambdaQueryWrapper<AiModelConfigEntity>()
                    .eq(AiModelConfigEntity::getEnabled, 1)
                    .eq(AiModelConfigEntity::getModelCode, altCode)
                    .last("limit 1"));
            if (Objects.nonNull(alt)) {
                return alt;
            }
        }
        throw new BusinessException(StatusCodeEnum.NOT_FOUND, "未找到可用模型配置: " + targetCode);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private Object invokeGetter(Object target, String methodName) {
        try {
            return target.getClass().getMethod(methodName).invoke(target);
        } catch (Exception e) {
            return null;
        }
    }

    private Integer toIntSafe(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private void recordBilling(Long userId,
                               String scene,
                               AiModelConfigEntity modelConfig,
                               String requestText,
                               ChatInvokeResult result,
                               String errMsg,
                               Long elapsedMs) {
        if (result == null) {
            return;
        }
        int requestChars = requestText == null ? 0 : requestText.length();
        int responseChars = result.answer == null ? 0 : result.answer.length();
        int promptTokens = result.promptTokens == null ? estimateTokensByChars(requestChars) : Math.max(0, result.promptTokens);
        int completionTokens = result.completionTokens == null ? estimateTokensByChars(responseChars) : Math.max(0, result.completionTokens);
        int totalTokens = result.totalTokens == null ? (promptTokens + completionTokens) : Math.max(0, result.totalTokens);
        String billingType = (result.promptTokens == null || result.completionTokens == null || result.totalTokens == null)
                ? "ESTIMATE_CHAR"
                : result.billingType;
        BigDecimal consumedPoints = BigDecimal.ZERO;
        if (errMsg == null && shouldCharge(userId, scene)) {
            consumedPoints = calcConsumedPoints(promptTokens, completionTokens, totalTokens);
            doDeductPowerPoints(userId, consumedPoints);
        }

        AiTokenBillingLogEntity log = new AiTokenBillingLogEntity();
        log.setUserId(userId);
        log.setScene(scene);
        log.setModelCode(modelConfig.getModelCode());
        log.setInvokeType(modelConfig.getInvokeType());
        log.setBillingType(billingType);
        log.setPromptTokens(promptTokens);
        log.setCompletionTokens(completionTokens);
        log.setTotalTokens(totalTokens);
        log.setRequestChars(requestChars);
        log.setResponseChars(responseChars);
        log.setConsumedPoints(consumedPoints);
        log.setElapsedMs(elapsedMs == null ? null : Math.max(0L, elapsedMs));
        log.setStatus(errMsg == null ? 1 : 0);
        log.setErrorMessage(truncate(errMsg, 1000));
        log.setExtJson(truncate(result.extJson, 4000));
        aiTokenBillingLogMapper.insert(log);
    }

    private void recordBillingFailure(Long userId,
                                      String scene,
                                      AiModelConfigEntity modelConfig,
                                      String requestText,
                                      String errMsg,
                                      Long elapsedMs) {
        ChatInvokeResult empty = ChatInvokeResult.of("", null, null, null, "ESTIMATE_CHAR", null);
        recordBilling(userId, scene, modelConfig, requestText, empty, errMsg, elapsedMs);
    }

    private int estimateTokensByChars(int chars) {
        if (chars <= 0) {
            return 0;
        }
        return (int) Math.ceil(chars / 1.5d);
    }

    private BigDecimal calcConsumedPoints(int promptTokens, int completionTokens, int totalTokens) {
        int safePrompt = Math.max(0, promptTokens);
        int safeCompletion = Math.max(0, completionTokens);
        int safeTotal = Math.max(0, totalTokens);

        // 计费 token：ceil(totalTokens * 1.5)
        int billedTotal = BigDecimal.valueOf(safeTotal)
                .multiply(BILLING_TOKEN_MULTIPLIER)
                .setScale(0, RoundingMode.CEILING)
                .intValue();

        int billedPrompt;
        int billedCompletion;
        if (safeTotal <= 0 || billedTotal <= 0) {
            billedPrompt = 0;
            billedCompletion = 0;
        } else {
            // 按 prompt/completion 占比分摊到 billedTotal，避免丢失输入/输出不同单价
            BigDecimal ratio = BigDecimal.valueOf(safePrompt)
                    .divide(BigDecimal.valueOf(safeTotal), 8, RoundingMode.HALF_UP);
            billedPrompt = BigDecimal.valueOf(billedTotal)
                    .multiply(ratio)
                    .setScale(0, RoundingMode.HALF_UP)
                    .intValue();
            billedPrompt = Math.max(0, Math.min(billedPrompt, billedTotal));
            billedCompletion = billedTotal - billedPrompt;
        }

        BigDecimal inCost = BigDecimal.valueOf(billedPrompt)
                .divide(new BigDecimal("1000"), 8, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(IN_PRICE_PER_1K));
        BigDecimal outCost = BigDecimal.valueOf(billedCompletion)
                .divide(new BigDecimal("1000"), 8, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(OUT_PRICE_PER_1K));

        BigDecimal cost = inCost.add(outCost);
        if (cost.compareTo(MIN_CHARGE_POINTS) < 0) {
            cost = MIN_CHARGE_POINTS;
        }
        // 统一保留两位小数，且向上取整到分（避免少扣）
        return cost.setScale(2, RoundingMode.CEILING);
    }

    private boolean shouldCharge(Long userId, String scene) {
        // 仅当存在真实微信用户ID时才可能扣费；
        // 后台管理测试或系统调用（userId<=0）保持不扣费。
        if (userId == null || userId <= 0L) {
            return false;
        }
        // 微信端 AI 对话免费：CHAT / RAG / RESUME 不扣费
        String s = safeScene(scene);
        return !FREE_SCENES.contains(s);
    }

    private void ensureMinimumChargeBalance(Long userId, String scene) {
        if (!shouldCharge(userId, scene)) {
            return;
        }
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
        if (points == null || points.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal p = points.setScale(2, RoundingMode.CEILING);
        int affected = wxUserMapper.deductPowerPoints(userId, p);
        if (affected <= 0) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "算力点不足，请先充值");
        }
    }

    private Long safeUserId(Long userId) {
        return userId == null ? -1L : userId;
    }

    private String safeScene(String scene) {
        if (isBlank(scene)) {
            return "CHAT";
        }
        return scene.trim().toUpperCase();
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max);
    }

    private static class ChatInvokeResult {
        private final String answer;
        private final Integer promptTokens;
        private final Integer completionTokens;
        private final Integer totalTokens;
        private final String billingType;
        private final String extJson;

        private ChatInvokeResult(String answer,
                                 Integer promptTokens,
                                 Integer completionTokens,
                                 Integer totalTokens,
                                 String billingType,
                                 String extJson) {
            this.answer = answer == null ? "" : answer;
            this.promptTokens = promptTokens;
            this.completionTokens = completionTokens;
            this.totalTokens = totalTokens;
            this.billingType = billingType == null ? "ESTIMATE_CHAR" : billingType;
            this.extJson = extJson;
        }

        private static ChatInvokeResult of(String answer,
                                           Integer promptTokens,
                                           Integer completionTokens,
                                           Integer totalTokens,
                                           String billingType,
                                           String extJson) {
            return new ChatInvokeResult(answer, promptTokens, completionTokens, totalTokens, billingType, extJson);
        }
    }
}

