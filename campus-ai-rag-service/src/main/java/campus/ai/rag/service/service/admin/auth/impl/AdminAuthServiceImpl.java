package campus.ai.rag.service.service.admin.auth.impl;

import campus.ai.rag.service.common.StatusCodeEnum;
import campus.ai.rag.service.common.auth.LoginTokenInfo;
import campus.ai.rag.service.dto.admin.AdminLoginDTO;
import campus.ai.rag.service.exception.BusinessException;
import campus.ai.rag.service.properties.JwtProperties;
import campus.ai.rag.service.service.admin.auth.AdminAuthService;
import campus.ai.rag.service.service.security.TokenStoreService;
import campus.ai.rag.service.utils.JwtUtils;
import campus.ai.rag.service.vo.admin.AdminLoginVO;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class AdminAuthServiceImpl implements AdminAuthService {

    private final AuthenticationManager authenticationManager;
    private final TokenStoreService tokenStoreService;
    private final JwtProperties jwtProperties;
    private final JwtUtils jwtUtils;

    public AdminAuthServiceImpl(AuthenticationManager authenticationManager,
                                TokenStoreService tokenStoreService,
                                JwtProperties jwtProperties,
                                JwtUtils jwtUtils) {
        this.authenticationManager = authenticationManager;
        this.tokenStoreService = tokenStoreService;
        this.jwtProperties = jwtProperties;
        this.jwtUtils = jwtUtils;
    }

    @Override
    public AdminLoginVO login(AdminLoginDTO dto) {
        if (Objects.isNull(dto) || isBlank(dto.getUsername()) || isBlank(dto.getPassword())) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "用户名或密码不能为空");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getUsername().trim(), dto.getPassword())
        );

        // 后台登录态需要绑定角色，供后续接口鉴权使用
        String roleCode = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("ROLE_ADMIN");

        String token = jwtUtils.createAdminToken(authentication.getName(), roleCode);

        // 登录成功后将 token 会话信息写入 Redis，过滤器统一校验此会话是否有效
        LoginTokenInfo tokenInfo = new LoginTokenInfo();
        tokenInfo.setUserId(authentication.getName());
        tokenInfo.setUsername(authentication.getName());
        tokenInfo.setRoleCode(roleCode);
        tokenInfo.setClientType("ADMIN");
        tokenStoreService.storeToken(token, tokenInfo);

        AdminLoginVO vo = new AdminLoginVO();
        vo.setToken(token);
        vo.setTokenName(jwtProperties.getOrTokenName());
        vo.setExpiresIn(tokenStoreService.getTokenTtlMillis());
        return vo;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

