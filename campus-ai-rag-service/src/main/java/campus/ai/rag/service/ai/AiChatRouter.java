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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    private static final int MIN_CHARGE_POINTS = 2;

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
        try {
            ChatInvokeResult result = invokeByType(modelConfig, promptedMessage);
            recordBilling(safeUserId(userId), safeScene(scene), modelConfig, promptedMessage, result, null);
            return result.answer;
        } catch (RuntimeException ex) {
            recordBillingFailure(safeUserId(userId), safeScene(scene), modelConfig, promptedMessage, ex.getMessage());
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
        try {
            ChatInvokeResult result = invokeByType(modelConfig, userMessage);
            recordBilling(safeUserId(userId), "INTERNAL", modelConfig, userMessage, result, null);
            return result.answer;
        } catch (RuntimeException ex) {
            recordBillingFailure(safeUserId(userId), "INTERNAL", modelConfig, userMessage, ex.getMessage());
            throw ex;
        }
    }

    public String getDefaultModelCode() {
        return defaultModelCode;
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

    private AiModelConfigEntity resolveModel(String modelCode) {
        String targetCode = (modelCode == null || modelCode.trim().isEmpty()) ? defaultModelCode : modelCode.trim();
        AiModelConfigEntity model = aiModelConfigMapper.selectOne(new LambdaQueryWrapper<AiModelConfigEntity>()
                .eq(AiModelConfigEntity::getEnabled, 1)
                .eq(AiModelConfigEntity::getModelCode, targetCode)
                .last("limit 1"));
        if (Objects.nonNull(model)) {
            return model;
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
                               String errMsg) {
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
        int consumedPoints = 0;
        if (errMsg == null && shouldCharge(userId, scene)) {
            consumedPoints = calcConsumedPoints(promptTokens, completionTokens);
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
        log.setStatus(errMsg == null ? 1 : 0);
        log.setErrorMessage(truncate(errMsg, 1000));
        log.setExtJson(truncate(result.extJson, 4000));
        aiTokenBillingLogMapper.insert(log);
    }

    private void recordBillingFailure(Long userId,
                                      String scene,
                                      AiModelConfigEntity modelConfig,
                                      String requestText,
                                      String errMsg) {
        ChatInvokeResult empty = ChatInvokeResult.of("", null, null, null, "ESTIMATE_CHAR", null);
        recordBilling(userId, scene, modelConfig, requestText, empty, errMsg);
    }

    private int estimateTokensByChars(int chars) {
        if (chars <= 0) {
            return 0;
        }
        return (int) Math.ceil(chars / 1.5d);
    }

    private int calcConsumedPoints(int promptTokens, int completionTokens) {
        double inCost = ((double) promptTokens / 1000d) * IN_PRICE_PER_1K;
        double outCost = ((double) completionTokens / 1000d) * OUT_PRICE_PER_1K;
        int cost = (int) Math.ceil(inCost + outCost);
        return Math.max(MIN_CHARGE_POINTS, cost);
    }

    private boolean shouldCharge(Long userId, String scene) {
        if (userId == null || userId <= 0L) {
            return false;
        }
        // INTERNAL 为系统内部调用（如摘要/标题），不单独扣点，避免重复扣费。
        return !"INTERNAL".equalsIgnoreCase(scene);
    }

    private void ensureMinimumChargeBalance(Long userId, String scene) {
        if (!shouldCharge(userId, scene)) {
            return;
        }
        WxUserEntity user = wxUserMapper.selectById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "用户不存在或已被禁用");
        }
        int balance = user.getPowerPoints() == null ? 0 : user.getPowerPoints();
        if (balance < MIN_CHARGE_POINTS) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "算力点不足，请先充值");
        }
    }

    private void doDeductPowerPoints(Long userId, int points) {
        if (points <= 0) {
            return;
        }
        int affected = wxUserMapper.deductPowerPoints(userId, points);
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

