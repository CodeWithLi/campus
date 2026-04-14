package campus.ai.rag.service.utils;

import campus.ai.rag.service.common.WXConstant;
import campus.ai.rag.service.config.WxConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Component
@Slf4j
public class ApiService {

    @Autowired
    private RestTemplate restTemplate;

    // 微信小程序配置：包含 appId / appSecret（从配置文件加载）
    @Autowired
    private WxConfig wxConfig;

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
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String,Object> map = objectMapper.readValue(resp, Map.class);

        // 记录微信返回结果（便于定位 openid 获取失败等问题）
        log.info("微信返回结果: {}", map);

        // 返回给上层：上层可基于 openid 进行用户注册/登录并生成本系统 token
        return map;
    }
}
