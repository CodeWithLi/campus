package campus.ai.rag.service.service.wx.auth.strategy.impl;

import campus.ai.rag.service.common.StatusCodeEnum;
import campus.ai.rag.service.dto.wx.WxLoginDTO;
import campus.ai.rag.service.entity.wx.WxUserAuthEntity;
import campus.ai.rag.service.entity.wx.WxUserEntity;
import campus.ai.rag.service.exception.BusinessException;
import campus.ai.rag.service.mapper.wx.WxUserAuthMapper;
import campus.ai.rag.service.mapper.wx.WxUserMapper;
import campus.ai.rag.service.service.wx.auth.strategy.WxLoginStrategy;
import campus.ai.rag.service.service.wx.auth.strategy.WxLoginTokenIssuer;
import campus.ai.rag.service.vo.admin.AdminLoginVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class PasswordWxLoginStrategy implements WxLoginStrategy {

    private static final String LOGIN_TYPE = "PASSWORD";
    private static final String AUTH_TYPE_PASSWORD = "PASSWORD";

    private final WxUserMapper wxUserMapper;
    private final WxUserAuthMapper wxUserAuthMapper;
    private final PasswordEncoder passwordEncoder;
    private final WxLoginTokenIssuer wxLoginTokenIssuer;

    public PasswordWxLoginStrategy(WxUserMapper wxUserMapper,
                                   WxUserAuthMapper wxUserAuthMapper,
                                   PasswordEncoder passwordEncoder,
                                   WxLoginTokenIssuer wxLoginTokenIssuer) {
        this.wxUserMapper = wxUserMapper;
        this.wxUserAuthMapper = wxUserAuthMapper;
        this.passwordEncoder = passwordEncoder;
        this.wxLoginTokenIssuer = wxLoginTokenIssuer;
    }

    @Override
    public String supportType() {
        return LOGIN_TYPE;
    }

    @Override
    public AdminLoginVO login(WxLoginDTO dto) {
        if (dto == null || isBlank(dto.getUsername()) || isBlank(dto.getPassword())) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "用户名或密码不能为空");
        }
        String username = dto.getUsername().trim();

        WxUserAuthEntity authEntity = wxUserAuthMapper.findByAuthTypeAndAuthKey(AUTH_TYPE_PASSWORD, username);
        if (Objects.isNull(authEntity) || !Objects.equals(authEntity.getEnabled(), 1)) {
            throw new BusinessException(StatusCodeEnum.UNAUTHORIZED, "账号不存在或已禁用");
        }
        if (!Objects.equals(authEntity.getAllowLogin(), 1)) {
            throw new BusinessException(StatusCodeEnum.FORBIDDEN, "当前账号暂不允许使用密码登录");
        }
        if (isBlank(authEntity.getAuthSecret()) || !passwordEncoder.matches(dto.getPassword(), authEntity.getAuthSecret())) {
            throw new BusinessException(StatusCodeEnum.UNAUTHORIZED, "用户名或密码错误");
        }

        WxUserEntity userEntity = wxUserMapper.selectById(authEntity.getUserId());
        if (Objects.isNull(userEntity) || !Objects.equals(userEntity.getStatus(), 1)) {
            throw new BusinessException(StatusCodeEnum.UNAUTHORIZED, "用户不存在或已禁用");
        }
        return wxLoginTokenIssuer.issue(userEntity);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

