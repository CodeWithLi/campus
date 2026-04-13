/**
 * 该文件的作用：提供 JWT 的创建与解析能力，供登录鉴权流程统一调用。
 * <p>
 * 包含两类入口：基于项目默认配置生成 Token，以及传入密钥/过期时间的通用静态生成方法。
 * </p>
 */
package campus.ai.rag.service.utils;


import campus.ai.rag.service.common.StatusCodeEnum;
import campus.ai.rag.service.exception.BusinessException;
import campus.ai.rag.service.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JwtUtils：令牌工具类，用于生成与解析 JWT。
 */
@Component
public class JwtUtils {

    private static final long TOKEN_TTL_TWO_HOURS_MILLIS = 2 * 60 * 60 * 1000L;


    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 使用系统默认配置生成 JWT。
     *
     * @param userId 用户 ID
     * @return JWT 字符串
     */
    public String createJwt(String userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        return createJwt(jwtProperties.getOrSecretKey(), jwtProperties.getOrTtl(), claims);
    }

    /**
     * 创建用户登录 Token（有效期一天）
     *
     * @param userId 用户ID
     * @return JWT Token
     */
    public String createUserToken(String userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        return createJwt(jwtProperties.getOrSecretKey(), TOKEN_TTL_TWO_HOURS_MILLIS, claims);
    }

    /**
     * 创建后台管理员 Token（包含角色信息）。
     *
     * @param userId   用户ID或用户名
     * @param roleCode 角色编码（如 ROLE_ADMIN / ROLE_SUPER_ADMIN）
     * @return JWT Token
     */
    public String createAdminToken(String userId, String roleCode) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("roleCode", roleCode);
        claims.put("clientType", "ADMIN");
        return createJwt(jwtProperties.getOrSecretKey(), TOKEN_TTL_TWO_HOURS_MILLIS, claims);
    }

    /**
     * 创建微信端 Token（轻量校验，仅识别用户身份）。
     *
     * @param userId 用户ID或用户名
     * @return JWT Token
     */
    public String createWxToken(String userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("clientType", "WX");
        return createJwt(jwtProperties.getOrSecretKey(), TOKEN_TTL_TWO_HOURS_MILLIS, claims);
    }

    /**
     * @param secretKey 密钥
     * @param ttlMillis 过期时间
     * @param claims    具体信息
     * @return JWT 字符串
     */
    /**
     * 创建 JWT 令牌（静态通用方法）。
     */
    public static String createJwt(String secretKey, long ttlMillis, Map<String, Object> claims) {
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;

        long ttlTime = System.currentTimeMillis() + ttlMillis;
        Date time = new Date(ttlTime);

        JwtBuilder jwtBuilder = Jwts.builder()
                .setClaims(claims)
                .signWith(signatureAlgorithm, secretKey)
                .setExpiration(time);

        return jwtBuilder.compact();
    }

    public static Claims praseJwt(String secretKey, String token) {
        Claims claims = null;
        try {
            claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (io.jsonwebtoken.security.SignatureException e) {
            throw new BusinessException(StatusCodeEnum.UNAUTHORIZED, "登录凭证签名无效，请重新登录");
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            throw new BusinessException(StatusCodeEnum.UNAUTHORIZED, "登录已过期，请重新登录");
        } catch (io.jsonwebtoken.MalformedJwtException e) {
            throw new BusinessException(StatusCodeEnum.UNAUTHORIZED, "登录凭证格式错误，请重新登录");
        } catch (io.jsonwebtoken.UnsupportedJwtException | io.jsonwebtoken.security.SecurityException e) {
            throw new BusinessException(StatusCodeEnum.UNAUTHORIZED, "登录凭证不受支持或校验失败，请重新登录");
        }

        return claims;
    }
}
