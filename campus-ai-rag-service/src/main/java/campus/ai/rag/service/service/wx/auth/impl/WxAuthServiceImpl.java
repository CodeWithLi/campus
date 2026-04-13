package campus.ai.rag.service.service.wx.auth.impl;

import campus.ai.rag.service.common.StatusCodeEnum;
import campus.ai.rag.service.common.auth.LoginTokenInfo;
import campus.ai.rag.service.dto.wx.WxLoginDTO;
import campus.ai.rag.service.entity.wx.WxUserEntity;
import campus.ai.rag.service.exception.BusinessException;
import campus.ai.rag.service.mapper.wx.WxUserMapper;
import campus.ai.rag.service.properties.JwtProperties;
import campus.ai.rag.service.service.security.TokenStoreService;
import campus.ai.rag.service.service.wx.auth.WxAuthService;
import campus.ai.rag.service.utils.JwtUtils;
import campus.ai.rag.service.vo.admin.AdminLoginVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class WxAuthServiceImpl implements WxAuthService {

    private final WxUserMapper wxUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenStoreService tokenStoreService;
    private final JwtProperties jwtProperties;
    private final JwtUtils jwtUtils;

    public WxAuthServiceImpl(WxUserMapper wxUserMapper,
                             PasswordEncoder passwordEncoder,
                             TokenStoreService tokenStoreService,
                             JwtProperties jwtProperties,
                             JwtUtils jwtUtils) {
        this.wxUserMapper = wxUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenStoreService = tokenStoreService;
        this.jwtProperties = jwtProperties;
        this.jwtUtils = jwtUtils;
    }

    @Override
    public AdminLoginVO login(WxLoginDTO dto) {
        if (Objects.isNull(dto) || isBlank(dto.getUsername()) || isBlank(dto.getPassword())) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "用户名或密码不能为空");
        }

        WxUserEntity user = wxUserMapper.findByUsername(dto.getUsername().trim());
        if (Objects.isNull(user) || user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(StatusCodeEnum.UNAUTHORIZED, "用户不存在或已禁用");
        }
        if (isBlank(user.getPassword()) || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(StatusCodeEnum.UNAUTHORIZED, "用户名或密码错误");
        }

        String token = jwtUtils.createWxToken(String.valueOf(user.getId()));

        // 微信端仅维护轻量登录态，不加载后台 RBAC 角色
        LoginTokenInfo tokenInfo = new LoginTokenInfo();
        tokenInfo.setUserId(String.valueOf(user.getId()));
        tokenInfo.setUsername(user.getUsername());
        tokenInfo.setClientType("WX");
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

