package campus.ai.rag.service.utils;

import campus.ai.rag.service.common.WXConstant;
import campus.ai.rag.service.config.WxConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class ApiService {
    private static final String WX_ACCESS_TOKEN_CACHE_KEY = "campus:wx:access_token";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private RestTemplate restTemplate;

    // 微信小程序配置：包含 appId / appSecret（从配置文件加载）
    @Autowired
    private WxConfig wxConfig;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 调用微信小程序登录接口
     * @param code 小程序端获取的临时登录 code
     * @return 返回微信返回的 openid 和 session_key 等信息
     */
    // 小程序登录：用临时 code 换取 openid/session_key（微信标准流程）
    // - code 由小程序端 wx.login() 获取，有时效性且只能使用一次
    // - 返回 map 中通常包含 openid、session_key，失败时可能包含 errcode/errmsg
    public Map<String, Object> wxLogin(String code) throws JsonProcessingException {
        // 构建请求 URL：拼接微信登录接口所需 query 参数
        // appid/secret：来自配置；js_code：前端传入；grant_type 固定为 authorization_code
        String requestUrl = UriComponentsBuilder.fromHttpUrl(WXConstant.WX_LOGIN_URL)
                .queryParam("appid", wxConfig.getAppId())
                .queryParam("secret", wxConfig.getAppSecret())
                .queryParam("js_code", code)
                .queryParam("grant_type", "authorization_code")
                .toUriString();

        // 记录请求 URL，便于排查（注意：生产环境可考虑脱敏/避免打印敏感参数）
        log.info("微信登录请求URL: {}", requestUrl);

        // 发起 GET 请求获取微信返回的 JSON 字符串
        String resp = restTemplate.getForObject(requestUrl, String.class);
        // 将 JSON 解析为 Map（键值为微信返回字段）
        Map<String,Object> map = OBJECT_MAPPER.readValue(resp, Map.class);

        // 记录微信返回结果（便于定位 openid 获取失败等问题）
        log.info("微信返回结果: {}", map);

        // 返回给上层：上层可基于 openid 进行用户注册/登录并生成本系统 token
        return map;
    }

    @SuppressWarnings("unchecked")
    public String getMiniAppAccessToken() {
        try {
            Object cached = redisTemplate.opsForValue().get(WX_ACCESS_TOKEN_CACHE_KEY);
            if (cached instanceof String && !((String) cached).isBlank()) {
                return ((String) cached).trim();
            }
        } catch (Exception e) {
            log.warn("读取微信access_token缓存失败: {}", e.getMessage());
        }
        String requestUrl = UriComponentsBuilder.fromHttpUrl("https://api.weixin.qq.com/cgi-bin/token")
                .queryParam("grant_type", "client_credential")
                .queryParam("appid", wxConfig.getAppId())
                .queryParam("secret", wxConfig.getAppSecret())
                .toUriString();
        String resp = restTemplate.getForObject(requestUrl, String.class);
        try {
            Map<String, Object> map = OBJECT_MAPPER.readValue(resp, Map.class);
            Object tokenObj = map.get("access_token");
            if (!(tokenObj instanceof String) || ((String) tokenObj).isBlank()) {
                log.warn("获取微信access_token失败: {}", map);
                return "";
            }
            String token = ((String) tokenObj).trim();
            int expiresIn = 7200;
            Object expiresObj = map.get("expires_in");
            if (expiresObj instanceof Number) {
                expiresIn = ((Number) expiresObj).intValue();
            }
            int cacheSeconds = Math.max(60, expiresIn - 300);
            redisTemplate.opsForValue().set(WX_ACCESS_TOKEN_CACHE_KEY, token, Duration.ofSeconds(cacheSeconds));
            return token;
        } catch (Exception e) {
            log.warn("解析微信access_token失败: {}", e.getMessage());
            return "";
        }
    }

    public boolean sendMiniAppSubscribeMessage(String openId,
                                               String templateId,
                                               String page,
                                               String miniprogramState,
                                               String lang,
                                               Map<String, String> dataMap) {
        if (isBlank(openId) || isBlank(templateId) || dataMap == null || dataMap.isEmpty()) {
            return false;
        }
        String accessToken = getMiniAppAccessToken();
        if (isBlank(accessToken)) {
            return false;
        }
        String requestUrl = "https://api.weixin.qq.com/cgi-bin/message/subscribe/send?access_token=" + accessToken;
        Map<String, Object> payload = new HashMap<>();
        payload.put("touser", openId.trim());
        payload.put("template_id", templateId.trim());
        payload.put("page", isBlank(page) ? "pages/notification-center/notification-center" : page.trim());
        payload.put("miniprogram_state", isBlank(miniprogramState) ? "formal" : miniprogramState.trim());
        payload.put("lang", isBlank(lang) ? "zh_CN" : lang.trim());
        Map<String, Object> data = new HashMap<>();
        for (Map.Entry<String, String> entry : dataMap.entrySet()) {
            if (isBlank(entry.getKey())) {
                continue;
            }
            Map<String, String> field = new HashMap<>();
            field.put("value", entry.getValue() == null ? "" : entry.getValue());
            data.put(entry.getKey(), field);
        }
        payload.put("data", data);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String jsonBody = OBJECT_MAPPER.writeValueAsString(payload);
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(requestUrl, HttpMethod.POST, entity, String.class);
            String resp = response.getBody();
            Map<String, Object> result = OBJECT_MAPPER.readValue(resp, Map.class);
            Object errCodeObj = result.get("errcode");
            int errCode = errCodeObj instanceof Number ? ((Number) errCodeObj).intValue() : -1;
            if (errCode != 0) {
                log.warn("微信订阅消息发送失败，openId={}, templateId={}, result={}", openId, templateId, result);
                return false;
            }
            return true;
        } catch (HttpStatusCodeException e) {
            log.warn("微信订阅消息发送HTTP异常: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            return false;
        } catch (Exception e) {
            log.warn("微信订阅消息发送异常: {}", e.getMessage());
            return false;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
