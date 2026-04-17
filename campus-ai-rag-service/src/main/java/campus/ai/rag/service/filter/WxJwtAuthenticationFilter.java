package campus.ai.rag.service.filter;

import campus.ai.rag.service.common.auth.LoginTokenInfo;
import campus.ai.rag.service.properties.JwtProperties;
import campus.ai.rag.service.service.security.TokenStoreService;
import campus.ai.rag.service.utils.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * 微信用户端 JWT 过滤器：用于 /wx/**，仅做轻量身份校验，不做 RBAC 权限装载。
 */
@Component
public class WxJwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProperties jwtProperties;
    private final TokenStoreService tokenStoreService;

    public WxJwtAuthenticationFilter(JwtProperties jwtProperties, TokenStoreService tokenStoreService) {
        this.jwtProperties = jwtProperties;
        this.tokenStoreService = tokenStoreService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 注意：应用配置了 context-path=/campus，getRequestURI() 会包含 /campus 前缀
        // 这里使用 servletPath（不含 context-path），避免过滤器因为前缀导致完全不生效
        String path = request.getServletPath();
        return !path.startsWith("/wx/") || path.startsWith("/wx/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = extractToken(request.getHeader(jwtProperties.getOrTokenName()));
        if (token == null || token.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            LoginTokenInfo tokenInfo = tokenStoreService.getTokenInfo(token);
            if (tokenInfo == null || !"WX".equalsIgnoreCase(tokenInfo.getClientType())) {
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }
            JwtUtils.praseJwt(jwtProperties.getOrSecretKey(), token);

            String principal = tokenInfo.getUserId();
            User user = new User(principal, "", Collections.emptyList());
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            tokenStoreService.refresh(token);
        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(String authHeader) {
        if (authHeader == null || authHeader.isBlank()) {
            return null;
        }
        if (authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }
        return authHeader.trim();
    }
}

