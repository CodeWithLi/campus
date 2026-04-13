package campus.ai.rag.service.config;

import campus.ai.rag.service.common.Result;
import campus.ai.rag.service.common.StatusCodeEnum;
import campus.ai.rag.service.filter.AdminJwtAuthenticationFilter;
import campus.ai.rag.service.filter.WxJwtAuthenticationFilter;
import campus.ai.rag.service.service.security.AdminUserDetailsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

/**
 * Spring Security 配置：
 * 1. /admin/** 作为后台接口，默认要求认证（登录接口放行）。
 * 2. /wx/** 作为微信小程序接口，当前先放行，后续可单独加鉴权策略。
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final AdminJwtAuthenticationFilter adminJwtAuthenticationFilter;
    private final WxJwtAuthenticationFilter wxJwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(AdminJwtAuthenticationFilter adminJwtAuthenticationFilter,
                          WxJwtAuthenticationFilter wxJwtAuthenticationFilter,
                          ObjectMapper objectMapper) {
        this.adminJwtAuthenticationFilter = adminJwtAuthenticationFilter;
        this.wxJwtAuthenticationFilter = wxJwtAuthenticationFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html", "/doc.html").permitAll()
                        .requestMatchers("/admin/auth/login").permitAll()
                        .requestMatchers("/wx/auth/login").permitAll()
                        .requestMatchers("/admin/**").authenticated()
                        .requestMatchers("/wx/**").authenticated()
                        .anyRequest().permitAll()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                writeJson(response, Result.error(StatusCodeEnum.UNAUTHORIZED)))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeJson(response, Result.error(StatusCodeEnum.FORBIDDEN)))
                )
                .addFilterBefore(adminJwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(wxJwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(AdminUserDetailsService adminUserDetailsService) {
        return adminUserDetailsService;
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(UserDetailsService userDetailsService,
                                                               PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    private void writeJson(HttpServletResponse response, Result<Object> result) throws IOException {
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}

