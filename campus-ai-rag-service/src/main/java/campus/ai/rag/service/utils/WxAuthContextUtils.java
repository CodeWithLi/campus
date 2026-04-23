package campus.ai.rag.service.utils;

import campus.ai.rag.service.common.StatusCodeEnum;
import campus.ai.rag.service.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

public final class WxAuthContextUtils {

    private WxAuthContextUtils() {
    }

    /**
     * 从 SecurityContext 中获取当前微信用户ID（由 WxJwtAuthenticationFilter 写入 principal）。
     */
    public static String requireWxUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new BusinessException(StatusCodeEnum.UNAUTHORIZED, "未登录或登录已过期");
        }
        Object principalObj = auth.getPrincipal();
        if (principalObj instanceof User) {
            String username = ((User) principalObj).getUsername();
            if (username != null && !username.trim().isEmpty()) {
                return username.trim();
            }
        }
        if (principalObj instanceof String) {
            String s = ((String) principalObj).trim();
            if (!s.isEmpty()) {
                return s;
            }
        }
        throw new BusinessException(StatusCodeEnum.UNAUTHORIZED, "未登录或登录已过期");
    }

    /**
     * 尝试获取当前微信用户ID；未登录时返回 null。
     */
    public static String getOptionalWxUserId() {
        try {
            return requireWxUserId();
        } catch (BusinessException e) {
            return null;
        }
    }
}

