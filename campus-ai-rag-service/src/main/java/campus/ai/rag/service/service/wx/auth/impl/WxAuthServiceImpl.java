package campus.ai.rag.service.service.wx.auth.impl;

import campus.ai.rag.service.common.StatusCodeEnum;
import campus.ai.rag.service.common.auth.LoginTokenInfo;
import campus.ai.rag.service.dto.wx.WxEmailCodeDTO;
import campus.ai.rag.service.dto.wx.WxRegisterDTO;
import campus.ai.rag.service.entity.wx.WxUserAuthEntity;
import campus.ai.rag.service.entity.wx.WxUserEntity;
import campus.ai.rag.service.exception.BusinessException;
import campus.ai.rag.service.mapper.wx.WxUserAuthMapper;
import campus.ai.rag.service.mapper.wx.WxUserMapper;
import campus.ai.rag.service.properties.JwtProperties;
import campus.ai.rag.service.service.security.TokenStoreService;
import campus.ai.rag.service.service.wx.auth.WxAuthService;
import campus.ai.rag.service.utils.ApiService;
import campus.ai.rag.service.utils.JwtUtils;
import campus.ai.rag.service.vo.admin.AdminLoginVO;
import campus.ai.rag.service.vo.wx.WxEmailCodeVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class WxAuthServiceImpl implements WxAuthService {

    private static final String AUTH_TYPE_PASSWORD = "PASSWORD";
    private static final String AUTH_TYPE_WX_OPENID = "WX_OPENID";
    private static final String REGISTER_SOURCE_PASSWORD = "PASSWORD";
    private static final String PROVIDER_WECHAT = "WECHAT";
    private static final String CLIENT_TYPE_WX = "WX";
    private static final String EMAIL_CODE_BIZ_REGISTER = "REGISTER";
    private static final Duration EMAIL_CODE_TTL = Duration.ofMinutes(5);

    private final WxUserMapper wxUserMapper;
    private final WxUserAuthMapper wxUserAuthMapper;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ApiService apiService;
    private final JwtUtils jwtUtils;
    private final JwtProperties jwtProperties;
    private final TokenStoreService tokenStoreService;

    public WxAuthServiceImpl(WxUserMapper wxUserMapper,
                             WxUserAuthMapper wxUserAuthMapper,
                             PasswordEncoder passwordEncoder,
                             RedisTemplate<String, Object> redisTemplate,
                             ApiService apiService,
                             JwtUtils jwtUtils,
                             JwtProperties jwtProperties,
                             TokenStoreService tokenStoreService) {
        this.wxUserMapper = wxUserMapper;
        this.wxUserAuthMapper = wxUserAuthMapper;
        this.passwordEncoder = passwordEncoder;
        this.redisTemplate = redisTemplate;
        this.apiService = apiService;
        this.jwtUtils = jwtUtils;
        this.jwtProperties = jwtProperties;
        this.tokenStoreService = tokenStoreService;
    }

    @Override
    public WxEmailCodeVO sendEmailCode(WxEmailCodeDTO dto) {
        if (Objects.isNull(dto) || isBlank(dto.getEmail())) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "邮箱不能为空");
        }
        String email = dto.getEmail().trim().toLowerCase();
        String code = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        redisTemplate.opsForValue().set(buildEmailCodeKey(email), code, EMAIL_CODE_TTL);

        WxEmailCodeVO vo = new WxEmailCodeVO();
        vo.setEmail(email);
        vo.setCode(code);
        vo.setExpireSeconds(EMAIL_CODE_TTL.toSeconds());
        return vo;
    }

    @Override
    public AdminLoginVO register(WxRegisterDTO dto) {
        validateRegisterDto(dto);

        String username = dto.getUsername().trim();
        String email = dto.getEmail().trim().toLowerCase();
        String emailCode = dto.getEmailCode().trim();
        String wxCode = dto.getWxCode().trim();

        Object cachedCodeObj = redisTemplate.opsForValue().get(buildEmailCodeKey(email));
        if (!(cachedCodeObj instanceof String cachedCode) || !cachedCode.equalsIgnoreCase(emailCode)) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "邮箱验证码错误或已过期");
        }

        WxUserEntity usernameExists = wxUserMapper.findByUsername(username);
        if (Objects.nonNull(usernameExists)) {
            throw new BusinessException(StatusCodeEnum.CONFLICT, "用户名已存在");
        }
        WxUserEntity emailExists = wxUserMapper.findByEmail(email);
        if (Objects.nonNull(emailExists)) {
            throw new BusinessException(StatusCodeEnum.CONFLICT, "邮箱已存在");
        }

        String openid = resolveOpenidByWxCode(wxCode);
        WxUserAuthEntity openidExists = wxUserAuthMapper.findByAuthTypeAndAuthKey(AUTH_TYPE_WX_OPENID, openid);
        if (Objects.nonNull(openidExists)) {
            throw new BusinessException(StatusCodeEnum.CONFLICT, "该微信账号已绑定其他用户");
        }

        WxUserEntity userEntity = new WxUserEntity();
        userEntity.setUsername(username);
        userEntity.setEmail(email);
        userEntity.setNickname(username);
        userEntity.setStatus(1);
        userEntity.setRegisterSource(REGISTER_SOURCE_PASSWORD);
        wxUserMapper.insert(userEntity);

        WxUserAuthEntity passwordAuth = new WxUserAuthEntity();
        passwordAuth.setUserId(userEntity.getId());
        passwordAuth.setAuthType(AUTH_TYPE_PASSWORD);
        passwordAuth.setAuthKey(username);
        passwordAuth.setAuthSecret(passwordEncoder.encode(dto.getPassword().trim()));
        passwordAuth.setEnabled(1);
        passwordAuth.setAllowLogin(1);
        wxUserAuthMapper.insert(passwordAuth);

        WxUserAuthEntity wxAuth = new WxUserAuthEntity();
        wxAuth.setUserId(userEntity.getId());
        wxAuth.setAuthType(AUTH_TYPE_WX_OPENID);
        wxAuth.setAuthKey(openid);
        wxAuth.setProvider(PROVIDER_WECHAT);
        wxAuth.setEnabled(1);
        wxAuth.setAllowLogin(1);
        wxUserAuthMapper.insert(wxAuth);

        redisTemplate.delete(buildEmailCodeKey(email));

        String token = jwtUtils.createWxToken(String.valueOf(userEntity.getId()));
        LoginTokenInfo tokenInfo = new LoginTokenInfo();
        tokenInfo.setUserId(String.valueOf(userEntity.getId()));
        tokenInfo.setUsername(userEntity.getUsername());
        tokenInfo.setClientType(CLIENT_TYPE_WX);
        tokenStoreService.storeToken(token, tokenInfo);

        AdminLoginVO vo = new AdminLoginVO();
        vo.setToken(token);
        vo.setTokenName(jwtProperties.getOrTokenName());
        vo.setExpiresIn(tokenStoreService.getTokenTtlMillis());
        return vo;
    }

    private String resolveOpenidByWxCode(String wxCode) {
        try {
            Map<String, Object> result = apiService.wxLogin(wxCode);
            Object openidObj = result == null ? null : result.get("openid");
            if (!(openidObj instanceof String openid) || isBlank(openid)) {
                String errmsg = result == null ? "微信返回为空" : String.valueOf(result.get("errmsg"));
                throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "获取openid失败：" + errmsg);
            }
            return openid.trim();
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "调用微信接口失败：" + ex.getMessage());
        }
    }

    private void validateRegisterDto(WxRegisterDTO dto) {
        if (Objects.isNull(dto)) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "注册参数不能为空");
        }
        if (isBlank(dto.getUsername())) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "用户名不能为空");
        }
        if (isBlank(dto.getPassword())) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "密码不能为空");
        }
        if (isBlank(dto.getEmail())) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "邮箱不能为空");
        }
        if (isBlank(dto.getEmailCode())) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "邮箱验证码不能为空");
        }
        if (isBlank(dto.getWxCode())) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "微信登录code不能为空");
        }
    }

    private String buildEmailCodeKey(String email) {
        return "campus:email:code:" + EMAIL_CODE_BIZ_REGISTER + ":" + email;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

