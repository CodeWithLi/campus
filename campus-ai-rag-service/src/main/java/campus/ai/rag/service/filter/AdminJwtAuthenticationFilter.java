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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * 后台管理端 JWT 过滤器：用于 /admin/**，解析 token 并写入角色权限。
 */
@Component
public class AdminJwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProperties jwtProperties;
    private final TokenStoreService tokenStoreService;

    public AdminJwtAuthenticationFilter(JwtProperties jwtProperties, TokenStoreService tokenStoreService) {
        this.jwtProperties = jwtProperties;
        this.tokenStoreService = tokenStoreService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return !uri.startsWith("/admin/") || uri.startsWith("/admin/auth/login");
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
            if (tokenInfo == null || !"ADMIN".equalsIgnoreCase(tokenInfo.getClientType())) {
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }
            JwtUtils.praseJwt(jwtProperties.getOrSecretKey(), token);

            String principal = tokenInfo.getUserId();
            String roleCode = tokenInfo.getRoleCode() == null ? "ROLE_ADMIN" : tokenInfo.getRoleCode();
            List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(roleCode));

            User user = new User(principal, "", authorities);
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

