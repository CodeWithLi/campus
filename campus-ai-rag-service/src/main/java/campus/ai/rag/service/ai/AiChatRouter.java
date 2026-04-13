package campus.ai.rag.service.ai;

import campus.ai.rag.service.common.StatusCodeEnum;
import campus.ai.rag.service.entity.model.AiModelConfigEntity;
import campus.ai.rag.service.exception.BusinessException;
import campus.ai.rag.service.mapper.model.AiModelConfigMapper;
import campus.ai.rag.service.utils.GenericHttpUtils;
import campus.ai.rag.service.utils.XfHttpUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

    private final String defaultModelCode;
    private final AiModelConfigMapper aiModelConfigMapper;

    public AiChatRouter(String defaultModelCode, AiModelConfigMapper aiModelConfigMapper) {
        this.defaultModelCode = defaultModelCode;
        this.aiModelConfigMapper = aiModelConfigMapper;
    }

    public String chat(String modelCode, String userMessage) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "消息内容不能为空");
        }

        AiModelConfigEntity modelConfig = resolveModel(modelCode);
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

    public String getDefaultModelCode() {
        return defaultModelCode;
    }

    public void validate() {
        Objects.requireNonNull(defaultModelCode, "defaultModelCode 不能为空");
        Objects.requireNonNull(aiModelConfigMapper, "aiModelConfigMapper 不能为空");
    }

    private String springAiChat(AiModelConfigEntity modelConfig, String userMessage) {
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
        return openAiChatModel.call(prompt).getResult().getOutput().getText();
    }

    private String xfHttpChat(AiModelConfigEntity modelConfig, String userMessage) {
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
        return XfHttpUtils.extractContent(response);
    }

    private String genericHttpChat(AiModelConfigEntity modelConfig, String userMessage) {
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

        return GenericHttpUtils.postChat(
                modelConfig.getApiBaseUrl(),
                modelConfig.getGenericHeadersJson(),
                reqType,
                modelConfig.getGenericReqField(),
                userMessage,
                modelConfig.getGenericRespPath()
        );
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
}

