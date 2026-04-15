package campus.ai.rag.service.service.security;

import campus.ai.rag.service.common.auth.LoginTokenInfo;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class TokenStoreService {

    private static final String TOKEN_KEY_PREFIX = "campus:auth:token:";
    private static final Duration TOKEN_TTL = Duration.ofHours(2);

    private final RedisTemplate<String, Object> redisTemplate;

    public TokenStoreService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void storeToken(String token, LoginTokenInfo tokenInfo) {
        tokenInfo.setToken(token);
        redisTemplate.opsForValue().set(buildKey(token), tokenInfo, TOKEN_TTL);
    }

    public LoginTokenInfo getTokenInfo(String token) {
        Object value = redisTemplate.opsForValue().get(buildKey(token));
        if (value instanceof LoginTokenInfo) {
            return (LoginTokenInfo) value;
        }
        return null;
    }

    public void refresh(String token) {
        redisTemplate.expire(buildKey(token), TOKEN_TTL);
    }

    public long getTokenTtlMillis() {
        return TOKEN_TTL.toMillis();
    }

    private String buildKey(String token) {
        return TOKEN_KEY_PREFIX + token;
    }
}

