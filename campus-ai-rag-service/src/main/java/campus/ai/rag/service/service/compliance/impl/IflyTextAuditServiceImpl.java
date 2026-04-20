package campus.ai.rag.service.service.compliance.impl;

import campus.ai.rag.service.common.StatusCodeEnum;
import campus.ai.rag.service.exception.BusinessException;
import campus.ai.rag.service.properties.IflyTextAuditProperties;
import campus.ai.rag.service.service.compliance.IflyTextAuditService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Service
public class IflyTextAuditServiceImpl implements IflyTextAuditService {

    private static final DateTimeFormatter UTC_FMT =
            DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ssZ").withZone(ZoneOffset.UTC);

    private final IflyTextAuditProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public IflyTextAuditServiceImpl(IflyTextAuditProperties properties, RestTemplate restTemplate) {
        this.properties = properties;
        this.restTemplate = restTemplate;
    }

    @Override
    public boolean isPass(String text) {
        if (text == null || text.isEmpty()) {
            return true;
        }
        if (!properties.isEnabled()) {
            return true;
        }
        if (!properties.isConfigured()) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "文本合规已启用但未配置完整 accessKeyId/accessKeySecret/appId");
        }
        try {
            String uuid = UUID.randomUUID().toString();
            String utc = UTC_FMT.format(ZonedDateTime.now(ZoneOffset.UTC));

            TreeMap<String, String> signParams = new TreeMap<>();
            signParams.put("accessKeyId", properties.getAccessKeyId().trim());
            signParams.put("accessKeySecret", properties.getAccessKeySecret().trim());
            signParams.put("appId", properties.getAppId().trim());
            signParams.put("utc", utc);
            signParams.put("uuid", uuid);

            StringBuilder baseString = new StringBuilder();
            for (Map.Entry<String, String> e : signParams.entrySet()) {
                if (e.getValue() == null || e.getValue().isEmpty()) {
                    continue;
                }
                if (baseString.length() > 0) {
                    baseString.append("&");
                }
                baseString.append(e.getKey())
                        .append("=")
                        .append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
            }

            String signature = hmacSha1Base64(properties.getAccessKeySecret().trim(), baseString.toString());

            String url = UriComponentsBuilder.fromUriString(properties.getUrl().trim())
                    .queryParam("accessKeyId", properties.getAccessKeyId().trim())
                    .queryParam("accessKeySecret", properties.getAccessKeySecret().trim())
                    .queryParam("utc", utc)
                    .queryParam("signature", signature)
                    .queryParam("appId", properties.getAppId().trim())
                    .queryParam("uuid", uuid)
                    .encode()
                    .build()
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ObjectNode body = objectMapper.createObjectNode();
            body.put("is_match_all", 1);
            body.put("content", text);
            ArrayNode cats = body.putArray("categories");
            cats.add("pornDetection");
            cats.add("violentTerrorism");
            cats.add("political");
            cats.add("lowQualityIrrigation");
            cats.add("contraband");
            cats.add("advertisement");
            cats.add("uncivilizedLanguage");
            String bodyJson = objectMapper.writeValueAsString(body);

            String resp = restTemplate.postForObject(url, new HttpEntity<>(bodyJson, headers), String.class);
            if (resp == null || resp.isEmpty()) {
                throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "文本合规接口无响应");
            }
            JsonNode root = objectMapper.readTree(resp);
            String code = text(root, "code");
            if (!"000000".equals(code)) {
                String desc = text(root, "desc");
                throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "文本合规调用失败：" + (desc == null ? code : desc));
            }
            JsonNode suggest = root.path("data").path("result").path("suggest");
            if (suggest.isMissingNode()) {
                return true;
            }
            return "pass".equalsIgnoreCase(suggest.asText());
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "文本合规调用异常：" + ex.getMessage());
        }
    }

    @Override
    public void assertPass(String text) {
        if (!isPass(text)) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "正文未通过文本合规审核，请修改后重试");
        }
    }

    private static String text(JsonNode root, String field) {
        JsonNode n = root.get(field);
        return n == null || n.isNull() ? null : n.asText();
    }

    private static String hmacSha1Base64(String secret, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
        byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(raw);
    }
}
