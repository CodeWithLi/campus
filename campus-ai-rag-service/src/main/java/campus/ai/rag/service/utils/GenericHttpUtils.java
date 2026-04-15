package campus.ai.rag.service.utils;

import campus.ai.rag.service.common.StatusCodeEnum;
import campus.ai.rag.service.exception.BusinessException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
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
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GenericHttpUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final Pattern PATH_TOKEN_PATTERN = Pattern.compile("([^.\\[\\]]+)(\\[(\\d+)])?");

    private GenericHttpUtils() {
    }

    public enum ReqType {
        MESSAGES,
        PROMPT,
        INPUT
    }

    public static String postChat(String endpointUrl,
                                  String headersJson,
                                  ReqType reqType,
                                  String reqField,
                                  String userMessage,
                                  String respPath) {
        validateRequired(endpointUrl, "endpointUrl");
        validateRequired(reqField, "reqField");
        validateRequired(userMessage, "userMessage");
        Objects.requireNonNull(reqType, "reqType 不能为空");

        Map<String, Object> body = new HashMap<>();
        switch (reqType) {
            case MESSAGES:
                body.put(reqField, List.of(Map.of("role", "user", "content", userMessage)));
                break;
            case PROMPT:
            case INPUT:
                body.put(reqField, userMessage);
                break;
            default:
                break;
        }

        Map<String, String> headers = parseHeaders(headersJson);
        String responseText = doPostJson(endpointUrl, headers, body);
        if (respPath == null || respPath.trim().isEmpty()) {
            return responseText;
        }
        return extractByPath(responseText, respPath.trim());
    }

    private static String doPostJson(String endpointUrl, Map<String, String> headers, Map<String, Object> body) {
        try {
            String bodyJson = OBJECT_MAPPER.writeValueAsString(body);
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(endpointUrl))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json");

            if (headers != null) {
                headers.forEach((k, v) -> {
                    if (k != null && !k.isBlank() && v != null) {
                        builder.header(k, v);
                    }
                });
            }

            HttpRequest request = builder
                    .POST(HttpRequest.BodyPublishers.ofString(bodyJson, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR,
                        "通用HTTP调用失败，status=" + response.statusCode() + "，body=" + response.body());
            }
            return response.body();
        } catch (IOException e) {
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "通用HTTP调用IO异常：" + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "通用HTTP调用被中断：" + e.getMessage());
        }
    }

    private static Map<String, String> parseHeaders(String headersJson) {
        if (headersJson == null || headersJson.trim().isEmpty()) {
            return Map.of();
        }
        try {
            Map<String, Object> map = OBJECT_MAPPER.readValue(headersJson, new TypeReference<>() {
            });
            Map<String, String> result = new HashMap<>();
            for (Map.Entry<String, Object> e : map.entrySet()) {
                if (e.getKey() != null && e.getValue() != null) {
                    result.put(e.getKey(), String.valueOf(e.getValue()));
                }
            }
            return result;
        } catch (Exception e) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "genericHeadersJson 不是合法JSON对象：" + e.getMessage());
        }
    }

    private static String extractByPath(String responseText, String path) {
        try {
            JsonNode node = OBJECT_MAPPER.readTree(responseText);
            JsonNode cur = node;
            for (String part : path.split("\\.")) {
                if (part.isBlank()) {
                    continue;
                }
                Matcher matcher = PATH_TOKEN_PATTERN.matcher(part);
                if (!matcher.matches()) {
                    return "";
                }
                String field = matcher.group(1);
                String indexStr = matcher.group(3);
                cur = cur == null ? null : cur.get(field);
                if (cur == null) {
                    return "";
                }
                if (indexStr != null) {
                    int idx = Integer.parseInt(indexStr);
                    cur = cur.isArray() && cur.size() > idx ? cur.get(idx) : null;
                    if (cur == null) {
                        return "";
                    }
                }
            }
            if (cur == null || cur.isNull()) {
                return "";
            }
            return cur.isTextual() ? cur.asText() : cur.toString();
        } catch (Exception e) {
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "通用HTTP响应解析失败：" + e.getMessage());
        }
    }

    private static void validateRequired(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, fieldName + " 不能为空");
        }
    }
}

