package campus.ai.rag.service.utils;

import campus.ai.rag.service.common.StatusCodeEnum;
import campus.ai.rag.service.exception.BusinessException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 讯飞简历生成 HTTP 调用工具（签名鉴权 + base64 请求/响应）。
 */
public final class XfResumeHttpUtils {
    private static final Logger log = LoggerFactory.getLogger(XfResumeHttpUtils.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    private XfResumeHttpUtils() {
    }

    public static ResumeResult generate(String endpointUrl,
                                        String appId,
                                        String apiKey,
                                        String apiSecret,
                                        String plainText) {
        if (isBlank(endpointUrl) || isBlank(appId) || isBlank(apiKey) || isBlank(apiSecret)) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "星火简历生成配置不完整");
        }
        if (isBlank(plainText)) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "简历文本不能为空");
        }
        try {
            URI uri = URI.create(endpointUrl);
            String host = uri.getHost();
            // 签名中的 path 以“解码后的 path”为准（避免 rawPath 编码差异导致验签失败）
            String path = uri.getPath();
            if (isBlank(path)) path = "/";
            // 按 http鉴权方式.md 要求：Date 必须是 GMT/UTC0 的 RFC1123 格式
            String date = DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneId.of("GMT")));
            // 文档要求：authorization 为 base64(authorization_origin)
            String authorizationB64 = buildAuthorization(host, path, date, apiKey, apiSecret, true);
            log.info("XF resume request init: endpoint={}, host={}, path={}, date={}", endpointUrl, host, path, date);
            log.info("XF resume auth(query) authorization(base64) prefix={}", safePrefix(authorizationB64, 48));

            Map<String, Object> root = new HashMap<>();
            Map<String, Object> header = new HashMap<>();
            header.put("app_id", appId);
            header.put("status", 3);
            root.put("header", header);

            Map<String, Object> parameter = new HashMap<>();
            Map<String, Object> aiResume = new HashMap<>();
            Map<String, Object> resData = new HashMap<>();
            resData.put("encoding", "utf8");
            resData.put("compress", "raw");
            resData.put("format", "json");
            aiResume.put("resData", resData);
            parameter.put("ai_resume", aiResume);
            root.put("parameter", parameter);

            Map<String, Object> payload = new HashMap<>();
            Map<String, Object> reqData = new HashMap<>();
            reqData.put("encoding", "utf8");
            reqData.put("compress", "raw");
            reqData.put("format", "plain");
            reqData.put("status", 3);
            reqData.put("text", Base64.getEncoder().encodeToString(plainText.getBytes(StandardCharsets.UTF_8)));
            payload.put("reqData", reqData);
            root.put("payload", payload);

            String body = OBJECT_MAPPER.writeValueAsString(root);
            // 按文档优先：URL query 携带 authorization/date/host
            URI queryAuthUri = appendAuthQuery(uri, authorizationB64, date, host);
            log.info("XF resume attempt#1 query-auth url={}", queryAuthUri);
            HttpResponse<String> response = sendQueryAuthOnce(queryAuthUri, body, date);
            log.info("XF resume attempt#1 result: status={}, body={}", response.statusCode(), safeSnippet(response.body(), 1200));
            if (response.statusCode() == 401) {
                // 兼容兜底：有些网关也允许 Header 传递（不带 Host 头）
                log.info("XF resume attempt#2 header-auth url={}", uri);
                response = sendOnce(uri, body, date, authorizationB64);
                log.info("XF resume attempt#2 result: status={}, body={}", response.statusCode(), safeSnippet(response.body(), 1200));
            }
            if (response.statusCode() == 401 && bodyContainsHostEnforced(response.body())) {
                // 进一步兼容：部分网关验签取到的 Host 可能带端口（例如 cn-huadong-1.xf-yun.com:443）
                int port = uri.getPort();
                int effectivePort = port > 0 ? port : ("https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80);
                String hostWithPort = host + ":" + effectivePort;
                String authorizationWithPort = buildAuthorization(hostWithPort, path, date, apiKey, apiSecret, true);
                log.info("XF resume host-enforced fallback: hostWithPort={}, authorization(base64) prefix={}", hostWithPort, safePrefix(authorizationWithPort, 48));

                URI queryAuthUri2 = appendAuthQuery(uri, authorizationWithPort, date, hostWithPort);
                log.info("XF resume attempt#3 query-auth(host:port) url={}", queryAuthUri2);
                response = sendQueryAuthOnce(queryAuthUri2, body, date);
                log.info("XF resume attempt#3 result: status={}, body={}", response.statusCode(), safeSnippet(response.body(), 1200));
                if (response.statusCode() == 401) {
                    log.info("XF resume attempt#4 header-auth(host:port) url={}", uri);
                    response = sendOnce(uri, body, date, authorizationWithPort);
                    log.info("XF resume attempt#4 result: status={}, body={}", response.statusCode(), safeSnippet(response.body(), 1200));
                }
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR,
                        "星火简历生成HTTP调用失败，status=" + response.statusCode() + "，body=" + response.body());
            }
            return parseResponse(response.body());
        } catch (BusinessException ex) {
            log.warn("XF resume business error: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("XF resume unexpected error", ex);
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "调用星火简历生成失败：" + ex.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private static ResumeResult parseResponse(String responseBody) throws Exception {
        Map<String, Object> root = OBJECT_MAPPER.readValue(responseBody, new TypeReference<>() {
        });
        Map<String, Object> header = (Map<String, Object>) root.get("header");
        Integer code = toInt(header == null ? null : header.get("code"));
        String message = header == null ? null : String.valueOf(header.get("message"));
        String sid = header == null ? null : String.valueOf(header.get("sid"));
        if (code == null || code != 0) {
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "星火简历生成失败：code=" + code + ", message=" + message + ", sid=" + sid);
        }

        Map<String, Object> payload = (Map<String, Object>) root.get("payload");
        Map<String, Object> resData = payload == null ? null : (Map<String, Object>) payload.get("resData");
        String textB64 = resData == null ? null : (String) resData.get("text");
        if (isBlank(textB64)) {
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "星火简历生成返回为空");
        }
        String decoded = new String(Base64.getDecoder().decode(textB64), StandardCharsets.UTF_8);
        Map<String, Object> decodedJson = OBJECT_MAPPER.readValue(decoded, new TypeReference<>() {
        });
        Object linksObj = decodedJson.get("links");

        ResumeResult result = new ResumeResult();
        result.sid = sid;
        result.links = linksObj instanceof List ? (List<Map<String, Object>>) linksObj : List.of();
        return result;
    }

    private static HttpResponse<String> sendOnce(URI uri, String body, String date, String authorization) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("Date", date)
                .header("Authorization", authorization)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static HttpResponse<String> sendQueryAuthOnce(URI uri, String body, String date) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("Date", date)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static URI appendAuthQuery(URI uri, String authorization, String date, String host) throws Exception {
        // 这里手动拼接 raw URL，避免「先 URLEncoder 再 new URI(..., query, ...)」导致 % 被再次编码成 %25
        String oldQuery = uri.getRawQuery();
        String rawPath = uri.getRawPath();
        if (isBlank(rawPath)) {
            rawPath = "/";
        }

        StringBuilder fullUrl = new StringBuilder();
        fullUrl.append(uri.getScheme()).append("://").append(uri.getAuthority()).append(rawPath).append("?");
        if (oldQuery != null && !oldQuery.isEmpty()) {
            fullUrl.append(oldQuery).append("&");
        }
        fullUrl.append("authorization=").append(urlEncode(authorization))
                .append("&date=").append(urlEncode(date))
                .append("&host=").append(urlEncode(host));
        return URI.create(fullUrl.toString());
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private static String safeSnippet(String text, int limit) {
        if (text == null) return "null";
        if (limit <= 0 || text.length() <= limit) return text;
        return text.substring(0, limit) + "...(truncated)";
    }

    private static String safePrefix(String text, int prefix) {
        if (text == null) return "null";
        if (prefix <= 0 || text.length() <= prefix) return text;
        return text.substring(0, prefix) + "...";
    }

    private static boolean bodyContainsHostEnforced(String body) {
        if (body == null) return false;
        String b = body.toLowerCase(Locale.ROOT);
        return b.contains("enforced header 'host'") || b.contains("enforced header \"host\"");
    }

    private static String buildAuthorization(String host,
                                             String path,
                                             String date,
                                             String apiKey,
                                             String apiSecret,
                                             boolean encodeWholeAuth) throws Exception {
        String signatureOrigin = "host: " + host + "\n"
                + "date: " + date + "\n"
                + "POST " + path + " HTTP/1.1";

        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] signBytes = mac.doFinal(signatureOrigin.getBytes(StandardCharsets.UTF_8));
        String signature = Base64.getEncoder().encodeToString(signBytes);

        String authOrigin = String.format(Locale.ROOT,
                "api_key=\"%s\", algorithm=\"hmac-sha256\", headers=\"host date request-line\", signature=\"%s\"",
                apiKey, signature);
        if (encodeWholeAuth) {
            return Base64.getEncoder().encodeToString(authOrigin.getBytes(StandardCharsets.UTF_8));
        }
        return authOrigin;
    }

    private static Integer toInt(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).intValue();
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isBlank(String v) {
        return v == null || v.trim().isEmpty();
    }

    public static class ResumeResult {
        public String sid;
        public List<Map<String, Object>> links = List.of();
    }
}

