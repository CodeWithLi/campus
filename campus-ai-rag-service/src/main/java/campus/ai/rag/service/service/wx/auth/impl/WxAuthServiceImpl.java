package campus.ai.rag.service.service.wx.auth.impl;

import campus.ai.rag.service.common.StatusCodeEnum;
import campus.ai.rag.service.dto.wx.WxEmailCodeDTO;
import campus.ai.rag.service.dto.wx.WxLoginDTO;
import campus.ai.rag.service.dto.wx.WxRegisterDTO;
import campus.ai.rag.service.entity.wx.WxUserAuthEntity;
import campus.ai.rag.service.entity.wx.WxUserEntity;
import campus.ai.rag.service.exception.BusinessException;
import campus.ai.rag.service.mapper.wx.WxUserAuthMapper;
import campus.ai.rag.service.mapper.wx.WxUserMapper;
import campus.ai.rag.service.service.wx.auth.WxAuthService;
import campus.ai.rag.service.service.wx.auth.strategy.WxLoginStrategy;
import campus.ai.rag.service.service.wx.auth.strategy.WxLoginTokenIssuer;
import campus.ai.rag.service.utils.ApiService;
import campus.ai.rag.service.vo.admin.AdminLoginVO;
import campus.ai.rag.service.vo.wx.WxEmailCodeVO;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class WxAuthServiceImpl implements WxAuthService {

    private static final String AUTH_TYPE_PASSWORD = "PASSWORD";
    private static final String AUTH_TYPE_WX_OPENID = "WX_OPENID";
    private static final String REGISTER_SOURCE_PASSWORD = "PASSWORD";
    private static final String PROVIDER_WECHAT = "WECHAT";
    private static final String EMAIL_CODE_BIZ_REGISTER = "REGISTER";
    private static final Duration EMAIL_CODE_TTL = Duration.ofMinutes(5);
    private static final Duration EMAIL_CODE_SEND_INTERVAL = Duration.ofSeconds(60);

    private final WxUserMapper wxUserMapper;
    private final WxUserAuthMapper wxUserAuthMapper;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ApiService apiService;
    private final WxLoginTokenIssuer wxLoginTokenIssuer;
    private final JavaMailSender javaMailSender;
    private final Map<String, WxLoginStrategy> loginStrategyMap;
    private final String mailFrom;

    public WxAuthServiceImpl(WxUserMapper wxUserMapper,
                             WxUserAuthMapper wxUserAuthMapper,
                             PasswordEncoder passwordEncoder,
                             RedisTemplate<String, Object> redisTemplate,
                             ApiService apiService,
                             WxLoginTokenIssuer wxLoginTokenIssuer,
                             JavaMailSender javaMailSender,
                             @Value("${spring.mail.username}") String mailFrom,
                             List<WxLoginStrategy> loginStrategies) {
        this.wxUserMapper = wxUserMapper;
        this.wxUserAuthMapper = wxUserAuthMapper;
        this.passwordEncoder = passwordEncoder;
        this.redisTemplate = redisTemplate;
        this.apiService = apiService;
        this.wxLoginTokenIssuer = wxLoginTokenIssuer;
        this.javaMailSender = javaMailSender;
        this.mailFrom = mailFrom;
        this.loginStrategyMap = loginStrategies.stream()
                .collect(Collectors.toMap(strategy -> strategy.supportType().toUpperCase(Locale.ROOT), Function.identity()));
    }

    @Override
    public WxEmailCodeVO sendEmailCode(WxEmailCodeDTO dto) {
        if (Objects.isNull(dto) || isBlank(dto.getEmail())) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "邮箱不能为空");
        }
        String email = dto.getEmail().trim().toLowerCase();
        validateEmailFormat(email);
        String frequencyKey = buildEmailCodeSendFrequencyKey(email);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(frequencyKey))) {
            throw new BusinessException(StatusCodeEnum.TOO_MANY_REQUESTS, "验证码发送过于频繁，请稍后再试");
        }
        String code = generateNumericCode();
        sendRegisterEmailCode(email, code);
        redisTemplate.opsForValue().set(buildEmailCodeKey(email), code, EMAIL_CODE_TTL);
        redisTemplate.opsForValue().set(frequencyKey, "1", EMAIL_CODE_SEND_INTERVAL);

        WxEmailCodeVO vo = new WxEmailCodeVO();
        vo.setEmail(email);
        vo.setExpireSeconds(EMAIL_CODE_TTL.toSeconds());
        vo.setMessage("验证码发送成功，请注意查收邮箱");
        return vo;
    }

    @Override
    public AdminLoginVO register(WxRegisterDTO dto) {
        validateRegisterDto(dto);

        String username = dto.getUsername().trim();
        String email = dto.getEmail().trim().toLowerCase();
        validateEmailFormat(email);
        String emailCode = dto.getEmailCode().trim();
        String wxCode = dto.getWxCode().trim();

        Object cachedCodeObj = redisTemplate.opsForValue().get(buildEmailCodeKey(email));
        if (!(cachedCodeObj instanceof String)) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "邮箱验证码错误或已过期");
        }
        String cachedCode = (String) cachedCodeObj;
        if (!cachedCode.equalsIgnoreCase(emailCode)) {
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
        userEntity.setPowerPoints(0);
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
        return wxLoginTokenIssuer.issue(userEntity);
    }

    @Override
    public AdminLoginVO login(WxLoginDTO dto) {
        if (Objects.isNull(dto) || isBlank(dto.getLoginType())) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "登录方式不能为空");
        }
        String loginType = dto.getLoginType().trim().toUpperCase(Locale.ROOT);
        WxLoginStrategy strategy = loginStrategyMap.get(loginType);
        if (Objects.isNull(strategy)) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "不支持的登录方式：" + loginType);
        }
        return strategy.login(dto);
    }

    private String resolveOpenidByWxCode(String wxCode) {
        try {
            Map<String, Object> result = apiService.wxLogin(wxCode);
            Object openidObj = result == null ? null : result.get("openid");
            if (!(openidObj instanceof String)) {
                String errmsg = result == null ? "微信返回为空" : String.valueOf(result.get("errmsg"));
                throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "获取openid失败：" + errmsg);
            }
            String openid = ((String) openidObj).trim();
            if (isBlank(openid)) {
                String errmsg = result == null ? "微信返回为空" : String.valueOf(result.get("errmsg"));
                throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "获取openid失败：" + errmsg);
            }
            return openid;
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

    private String buildEmailCodeSendFrequencyKey(String email) {
        return "campus:email:send:limit:" + EMAIL_CODE_BIZ_REGISTER + ":" + email;
    }

    private String generateNumericCode() {
        int code = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return String.valueOf(code);
    }

    private void sendRegisterEmailCode(String email, String code) {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
            helper.setFrom(mailFrom);
            helper.setTo(email);
            helper.setSubject("校园AI助手 - 注册验证码");
            helper.setText("您的注册验证码是：" + code + "，5分钟内有效。若非本人操作请忽略本邮件。", false);
            javaMailSender.send(mimeMessage);
        } catch (MessagingException ex) {
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "验证码邮件发送失败：" + ex.getMessage());
        } catch (Exception ex) {
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "邮件服务异常，请稍后重试");
        }
    }

    private void validateEmailFormat(String email) {
        if (!email.contains("@") || !email.contains(".")) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "邮箱格式不正确");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

