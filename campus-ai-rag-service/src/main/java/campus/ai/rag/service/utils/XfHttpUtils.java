package campus.ai.rag.service.utils;

import campus.ai.rag.service.common.StatusCodeEnum;
import campus.ai.rag.service.exception.BusinessException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 讯飞星火 HTTP 调用工具（兼容 OpenAI Chat Completions 协议）。
 */
public final class XfHttpUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private XfHttpUtils() {
    }

    /**
     * 非流式调用星火模型，返回完整响应 JSON 文本。
     *
     * @param endpointUrl 例如 https://spark-api-open.xf-yun.com/v2/chat/completions
     * @param apiPassword 控制台 APIpassword（请求头需 Bearer）
     * @param model       例如 spark-x
     * @param userId      用户标识
     * @param messages    对话消息数组，元素格式：{role,content}
     */
    public static String chatCompletions(String endpointUrl,
                                         String apiPassword,
                                         String model,
                                         String userId,
                                         List<Map<String, Object>> messages) {
        validateRequired(endpointUrl, "endpointUrl");
        validateRequired(apiPassword, "apiPassword");
        validateRequired(model, "model");
        validateRequired(userId, "userId");
        if (messages == null || messages.isEmpty()) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "messages 不能为空");
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("user", userId);
            body.put("messages", messages);
            body.put("stream", false);

            String bodyJson = OBJECT_MAPPER.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpointUrl))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiPassword)
                    .POST(HttpRequest.BodyPublishers.ofString(bodyJson, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR,
                        "讯飞HTTP调用失败，status=" + response.statusCode() + "，body=" + response.body());
            }
            return response.body();
        } catch (IOException e) {
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "讯飞HTTP调用IO异常：" + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "讯飞HTTP调用被中断：" + e.getMessage());
        }
    }

    /**
     * 从非流式响应中抽取 content 文本（仅取 choices[0].message.content）。
     */
    public static String extractContent(String responseJson) {
        validateRequired(responseJson, "responseJson");
        try {
            Map<String, Object> root = OBJECT_MAPPER.readValue(responseJson, new TypeReference<>() {
            });
            Object codeObj = root.get("code");
            if (codeObj instanceof Number code && code.intValue() != 0) {
                throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR,
                        "讯飞返回错误，code=" + code + "，message=" + root.get("message"));
            }

            Object choicesObj = root.get("choices");
            if (!(choicesObj instanceof List<?> choices) || choices.isEmpty()) {
                return "";
            }

            Object first = choices.get(0);
            if (!(first instanceof Map<?, ?> firstMap)) {
                return "";
            }
            Object messageObj = firstMap.get("message");
            if (!(messageObj instanceof Map<?, ?> messageMap)) {
                return "";
            }
            Object contentObj = messageMap.get("content");
            return contentObj == null ? "" : String.valueOf(contentObj);
        } catch (IOException e) {
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "解析讯飞响应失败：" + e.getMessage());
        }
    }

    private static void validateRequired(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, fieldName + " 不能为空");
        }
    }
}

