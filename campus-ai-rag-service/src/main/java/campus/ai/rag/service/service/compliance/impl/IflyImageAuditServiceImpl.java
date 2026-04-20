package campus.ai.rag.service.service.compliance.impl;

import campus.ai.rag.service.common.StatusCodeEnum;
import campus.ai.rag.service.exception.BusinessException;
import campus.ai.rag.service.properties.IflyImageAuditProperties;
import campus.ai.rag.service.service.compliance.IflyImageAuditService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Service
public class IflyImageAuditServiceImpl implements IflyImageAuditService {

    private static final DateTimeFormatter UTC_FMT =
            DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ssZ").withZone(ZoneOffset.UTC);
    private static final String PASS_CACHE_KEY_PREFIX = "campus:wx:post:image:audit:pass:";

    private final IflyImageAuditProperties properties;
    private final RestTemplate restTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public IflyImageAuditServiceImpl(IflyImageAuditProperties properties,
                                     RestTemplate restTemplate,
                                     RedisTemplate<String, Object> redisTemplate) {
        this.properties = properties;
        this.restTemplate = restTemplate;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean isPass(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            return true;
        }
        if (!properties.isEnabled()) {
            return true;
        }
        if (!properties.isConfigured()) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "图片合规已启用但未配置完整 accessKeyId/accessKeySecret/appId");
        }
        String imageHash = sha256Hex(imageBytes);
        String cacheKey = PASS_CACHE_KEY_PREFIX + imageHash;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey))) {
            return true;
        }

        try {
            String uuid = UUID.randomUUID().toString();
            String utc = UTC_FMT.format(ZonedDateTime.now(ZoneOffset.UTC));
            String modeType = "base64";

            TreeMap<String, String> signParams = new TreeMap<>();
            signParams.put("accessKeyId", properties.getAccessKeyId().trim());
            signParams.put("accessKeySecret", properties.getAccessKeySecret().trim());
            signParams.put("appId", properties.getAppId().trim());
            signParams.put("modeType", modeType);
            signParams.put("utc", utc);
            signParams.put("uuid", uuid);
            String baseString = buildBaseString(signParams);
            String signature = hmacSha1Base64(properties.getAccessKeySecret().trim(), baseString);

            String url = UriComponentsBuilder.fromUriString(properties.getUrl().trim())
                    .queryParam("accessKeyId", properties.getAccessKeyId().trim())
                    .queryParam("accessKeySecret", properties.getAccessKeySecret().trim())
                    .queryParam("appId", properties.getAppId().trim())
                    .queryParam("modeType", modeType)
                    .queryParam("utc", utc)
                    .queryParam("uuid", uuid)
                    .queryParam("signature", signature)
                    .encode()
                    .build()
                    .toUriString();

            ObjectNode body = objectMapper.createObjectNode();
            body.put("content", Base64.getEncoder().encodeToString(imageBytes));
            String bodyJson = objectMapper.writeValueAsString(body);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String resp = restTemplate.postForObject(url, new HttpEntity<>(bodyJson, headers), String.class);
            if (resp == null || resp.isEmpty()) {
                throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "图片合规接口无响应");
            }
            JsonNode root = objectMapper.readTree(resp);
            String code = text(root, "code");
            if (!"000000".equals(code)) {
                throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "图片合规调用失败：" + text(root, "desc"));
            }
            String suggest = root.path("data").path("result").path("suggest").asText("");
            boolean pass = "pass".equalsIgnoreCase(suggest);
            if (pass) {
                int days = Math.max(1, properties.getPassCacheDays());
                redisTemplate.opsForValue().set(cacheKey, 1, Duration.ofDays(days));
            }
            return pass;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "图片合规调用异常：" + ex.getMessage());
        }
    }

    @Override
    public void assertPass(byte[] imageBytes) {
        if (!isPass(imageBytes)) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "图片未通过合规审核，请更换后重试");
        }
    }

    private static String buildBaseString(TreeMap<String, String> params) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : params.entrySet()) {
            String value = e.getValue();
            if (value == null || value.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(e.getKey()).append("=").append(URLEncoder.encode(value, StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    private static String text(JsonNode root, String field) {
        JsonNode n = root.get(field);
        return n == null || n.isNull() ? "" : n.asText("");
    }

    private static String hmacSha1Base64(String secret, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
        byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(raw);
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(bytes));
        } catch (Exception e) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "图片指纹计算失败");
        }
    }
}
