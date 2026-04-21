package campus.ai.rag.service.service.wx.auth.impl;

import campus.ai.rag.service.common.StatusCodeEnum;
import campus.ai.rag.service.dto.wx.WxEmailCodeDTO;
import campus.ai.rag.service.dto.wx.WxLoginDTO;
import campus.ai.rag.service.dto.wx.WxNicknameUpdateDTO;
import campus.ai.rag.service.dto.wx.WxPasswordUpdateDTO;
import campus.ai.rag.service.dto.wx.WxRegisterDTO;
import campus.ai.rag.service.entity.wx.WxUserAuthEntity;
import campus.ai.rag.service.entity.wx.WxUserEntity;
import campus.ai.rag.service.entity.model.AiTokenBillingLogEntity;
import campus.ai.rag.service.exception.BusinessException;
import campus.ai.rag.service.mapper.model.AiTokenBillingLogMapper;
import campus.ai.rag.service.mapper.wx.WxUserAuthMapper;
import campus.ai.rag.service.mapper.wx.WxUserMapper;
import campus.ai.rag.service.service.wx.auth.WxAuthService;
import campus.ai.rag.service.service.wx.auth.strategy.WxLoginStrategy;
import campus.ai.rag.service.service.wx.auth.strategy.WxLoginTokenIssuer;
import campus.ai.rag.service.utils.ApiService;
import campus.ai.rag.service.utils.WxAuthContextUtils;
import campus.ai.rag.service.vo.admin.AdminLoginVO;
import campus.ai.rag.service.vo.wx.WxEmailCodeVO;
import campus.ai.rag.service.vo.wx.WxPowerPointLogPageVO;
import campus.ai.rag.service.vo.wx.WxPowerPointLogVO;
import campus.ai.rag.service.vo.wx.WxProfileVO;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
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
    private static final String EMAIL_CODE_BIZ_RESET_PASSWORD = "RESET_PASSWORD";
    private static final String VERIFY_TYPE_EMAIL_CODE = "EMAIL_CODE";
    private static final String VERIFY_TYPE_WX_ONE_CLICK = "WX_ONE_CLICK";
    private static final String DEFAULT_AVATAR_URL = "/avatar/defaultAvatar.jpg";
    private static final Duration EMAIL_CODE_TTL = Duration.ofMinutes(5);
    private static final Duration EMAIL_CODE_SEND_INTERVAL = Duration.ofSeconds(60);

    private final WxUserMapper wxUserMapper;
    private final AiTokenBillingLogMapper aiTokenBillingLogMapper;
    private final WxUserAuthMapper wxUserAuthMapper;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ApiService apiService;
    private final WxLoginTokenIssuer wxLoginTokenIssuer;
    private final JavaMailSender javaMailSender;
    private final Map<String, WxLoginStrategy> loginStrategyMap;
    private final String mailFrom;

    public WxAuthServiceImpl(WxUserMapper wxUserMapper,
                             AiTokenBillingLogMapper aiTokenBillingLogMapper,
                             WxUserAuthMapper wxUserAuthMapper,
                             PasswordEncoder passwordEncoder,
                             RedisTemplate<String, Object> redisTemplate,
                             ApiService apiService,
                             WxLoginTokenIssuer wxLoginTokenIssuer,
                             JavaMailSender javaMailSender,
                             @Value("${spring.mail.username}") String mailFrom,
                             List<WxLoginStrategy> loginStrategies) {
        this.wxUserMapper = wxUserMapper;
        this.aiTokenBillingLogMapper = aiTokenBillingLogMapper;
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
        String frequencyKey = buildEmailCodeSendFrequencyKey(email, EMAIL_CODE_BIZ_REGISTER);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(frequencyKey))) {
            throw new BusinessException(StatusCodeEnum.TOO_MANY_REQUESTS, "验证码发送过于频繁，请稍后再试");
        }
        String code = generateNumericCode();
        sendRegisterEmailCode(email, code);
        redisTemplate.opsForValue().set(buildEmailCodeKey(email, EMAIL_CODE_BIZ_REGISTER), code, EMAIL_CODE_TTL);
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

        Object cachedCodeObj = redisTemplate.opsForValue().get(buildEmailCodeKey(email, EMAIL_CODE_BIZ_REGISTER));
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
        userEntity.setNickname(generateDefaultNickname());
        userEntity.setAvatarUrl(DEFAULT_AVATAR_URL);
        userEntity.setStatus(1);
        userEntity.setRegisterSource(REGISTER_SOURCE_PASSWORD);
        userEntity.setPowerPoints(BigDecimal.ZERO);
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

        redisTemplate.delete(buildEmailCodeKey(email, EMAIL_CODE_BIZ_REGISTER));
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

    @Override
    public WxEmailCodeVO sendPasswordUpdateCode() {
        Long userId = parseUserId(WxAuthContextUtils.requireWxUserId());
        WxUserEntity user = wxUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(StatusCodeEnum.NOT_FOUND, "用户不存在");
        }
        String email = safeEmail(user.getEmail());
        if (isBlank(email)) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "当前账号未绑定邮箱，无法使用验证码修改密码");
        }
        String frequencyKey = buildEmailCodeSendFrequencyKey(email, EMAIL_CODE_BIZ_RESET_PASSWORD);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(frequencyKey))) {
            throw new BusinessException(StatusCodeEnum.TOO_MANY_REQUESTS, "验证码发送过于频繁，请稍后再试");
        }
        String code = generateNumericCode();
        sendResetPasswordEmailCode(email, code);
        redisTemplate.opsForValue().set(buildEmailCodeKey(email, EMAIL_CODE_BIZ_RESET_PASSWORD), code, EMAIL_CODE_TTL);
        redisTemplate.opsForValue().set(frequencyKey, "1", EMAIL_CODE_SEND_INTERVAL);

        WxEmailCodeVO vo = new WxEmailCodeVO();
        vo.setEmail(email);
        vo.setExpireSeconds(EMAIL_CODE_TTL.toSeconds());
        vo.setMessage("验证码发送成功，请注意查收邮箱");
        return vo;
    }

    @Override
    public Boolean updatePassword(WxPasswordUpdateDTO dto) {
        if (Objects.isNull(dto) || isBlank(dto.getVerifyType()) || isBlank(dto.getNewPassword())) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "参数不完整");
        }
        String verifyType = dto.getVerifyType().trim().toUpperCase(Locale.ROOT);
        String newPassword = dto.getNewPassword().trim();
        if (newPassword.length() < 6) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "新密码至少6位");
        }

        Long userId = parseUserId(WxAuthContextUtils.requireWxUserId());
        WxUserEntity user = wxUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(StatusCodeEnum.NOT_FOUND, "用户不存在");
        }
        WxUserAuthEntity passwordAuth = wxUserAuthMapper.findByUserIdAndAuthType(userId, AUTH_TYPE_PASSWORD);
        if (passwordAuth == null) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "当前账号未绑定密码登录");
        }

        if (VERIFY_TYPE_EMAIL_CODE.equals(verifyType)) {
            verifyByEmailCode(user, dto.getEmailCode());
        } else if (VERIFY_TYPE_WX_ONE_CLICK.equals(verifyType)) {
            verifyByWxOneClick(userId, dto.getWxCode());
        } else {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "不支持的校验方式");
        }

        passwordAuth.setAuthSecret(passwordEncoder.encode(newPassword));
        wxUserAuthMapper.updateById(passwordAuth);
        return Boolean.TRUE;
    }

    @Override
    public WxProfileVO getProfile() {
        Long userId = parseUserId(WxAuthContextUtils.requireWxUserId());
        WxUserEntity user = wxUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(StatusCodeEnum.NOT_FOUND, "用户不存在");
        }
        WxProfileVO vo = new WxProfileVO();
        vo.setUserId(user.getId());
        vo.setUsername(trimOrEmpty(user.getUsername()));
        vo.setEmail(trimOrEmpty(user.getEmail()));
        vo.setNickname(trimOrEmpty(user.getNickname()));
        String avatar = trimOrEmpty(user.getAvatarUrl());
        vo.setAvatarUrl(isBlank(avatar) ? DEFAULT_AVATAR_URL : avatar);
        vo.setPowerPoints(user.getPowerPoints() == null ? BigDecimal.ZERO : user.getPowerPoints());
        return vo;
    }

    @Override
    public WxPowerPointLogPageVO listPowerPointLogs(Long pageNum, Long pageSize) {
        Long userId = parseUserId(WxAuthContextUtils.requireWxUserId());
        long pNum = (pageNum == null || pageNum < 1) ? 1L : pageNum;
        long pSize = (pageSize == null || pageSize < 1) ? 20L : pageSize;
        pSize = Math.min(pSize, 50L);

        LambdaQueryWrapper<AiTokenBillingLogEntity> qw = new LambdaQueryWrapper<AiTokenBillingLogEntity>()
                .eq(AiTokenBillingLogEntity::getUserId, userId)
                .eq(AiTokenBillingLogEntity::getStatus, 1)
                .gt(AiTokenBillingLogEntity::getConsumedPoints, 0)
                .orderByDesc(AiTokenBillingLogEntity::getCreatedAt)
                .orderByDesc(AiTokenBillingLogEntity::getId);

        Page<AiTokenBillingLogEntity> page = aiTokenBillingLogMapper.selectPage(new Page<>(pNum, pSize), qw);
        List<WxPowerPointLogVO> list = page.getRecords().stream().map(this::toPowerPointLogVO).toList();

        WxPowerPointLogPageVO vo = new WxPowerPointLogPageVO();
        vo.setPageNum(pNum);
        vo.setPageSize(pSize);
        vo.setTotal(page.getTotal());
        vo.setList(list);
        return vo;
    }

    @Override
    public String uploadAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "头像文件不能为空");
        }
        Long userId = parseUserId(WxAuthContextUtils.requireWxUserId());
        WxUserEntity user = wxUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(StatusCodeEnum.NOT_FOUND, "用户不存在");
        }
        String ext = extension(file.getOriginalFilename());
        if (!isAllowedImageExt(ext)) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "仅支持 png/jpg/jpeg/bmp/gif/webp 图片");
        }
        long maxBytes = 5L * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "头像大小不能超过5MB");
        }
        String fileName = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8) + "." + ext;
        Path dir = Paths.get("upload", "avatar", String.valueOf(userId));
        try {
            Files.createDirectories(dir);
            Path target = dir.resolve(fileName);
            Files.write(target, file.getBytes());
            String avatarUrl = "/upload/avatar/" + userId + "/" + fileName;
            WxUserEntity update = new WxUserEntity();
            update.setId(userId);
            update.setAvatarUrl(avatarUrl);
            wxUserMapper.updateById(update);
            return avatarUrl;
        } catch (IOException e) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "头像保存失败");
        }
    }

    @Override
    public Boolean updateNickname(WxNicknameUpdateDTO dto) {
        if (dto == null || isBlank(dto.getNickname())) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "昵称不能为空");
        }
        String nickname = dto.getNickname().trim();
        if (nickname.length() < 2) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "昵称至少2个字符");
        }
        if (nickname.length() > 20) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "昵称不能超过20个字符");
        }
        Long userId = parseUserId(WxAuthContextUtils.requireWxUserId());
        WxUserEntity user = wxUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(StatusCodeEnum.NOT_FOUND, "用户不存在");
        }
        WxUserEntity update = new WxUserEntity();
        update.setId(userId);
        update.setNickname(nickname);
        return wxUserMapper.updateById(update) > 0;
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

    private String buildEmailCodeKey(String email, String biz) {
        return "campus:email:code:" + biz + ":" + email;
    }

    private String buildEmailCodeSendFrequencyKey(String email, String biz) {
        return "campus:email:send:limit:" + biz + ":" + email;
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

    private void sendResetPasswordEmailCode(String email, String code) {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
            helper.setFrom(mailFrom);
            helper.setTo(email);
            helper.setSubject("校园AI助手 - 修改密码验证码");
            helper.setText("您的修改密码验证码是：" + code + "，5分钟内有效。若非本人操作请忽略本邮件。", false);
            javaMailSender.send(mimeMessage);
        } catch (MessagingException ex) {
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "验证码邮件发送失败：" + ex.getMessage());
        } catch (Exception ex) {
            throw new BusinessException(StatusCodeEnum.BUSINESS_ERROR, "邮件服务异常，请稍后重试");
        }
    }

    private void verifyByEmailCode(WxUserEntity user, String emailCode) {
        String email = safeEmail(user.getEmail());
        if (isBlank(email)) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "当前账号未绑定邮箱");
        }
        if (isBlank(emailCode)) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "邮箱验证码不能为空");
        }
        Object cachedCodeObj = redisTemplate.opsForValue().get(buildEmailCodeKey(email, EMAIL_CODE_BIZ_RESET_PASSWORD));
        if (!(cachedCodeObj instanceof String)) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "邮箱验证码错误或已过期");
        }
        String cachedCode = (String) cachedCodeObj;
        if (!cachedCode.equalsIgnoreCase(emailCode.trim())) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "邮箱验证码错误或已过期");
        }
        redisTemplate.delete(buildEmailCodeKey(email, EMAIL_CODE_BIZ_RESET_PASSWORD));
    }

    private void verifyByWxOneClick(Long userId, String wxCode) {
        if (isBlank(wxCode)) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "微信校验参数不能为空");
        }
        String openid = resolveOpenidByWxCode(wxCode.trim());
        WxUserAuthEntity wxAuth = wxUserAuthMapper.findByUserIdAndAuthType(userId, AUTH_TYPE_WX_OPENID);
        if (wxAuth == null || isBlank(wxAuth.getAuthKey())) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "当前账号未绑定微信一键登录");
        }
        if (!openid.equals(wxAuth.getAuthKey())) {
            throw new BusinessException(StatusCodeEnum.FORBIDDEN, "微信校验失败：非当前账号绑定的微信");
        }
    }

    private String safeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private void validateEmailFormat(String email) {
        if (!email.contains("@") || !email.contains(".")) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "邮箱格式不正确");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String trimOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static Long parseUserId(String wxUserId) {
        try {
            return Long.parseLong(wxUserId);
        } catch (Exception e) {
            throw new BusinessException(StatusCodeEnum.UNAUTHORIZED, "用户未登录");
        }
    }

    private String generateDefaultNickname() {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        if (suffix.length() > 6) {
            suffix = suffix.substring(suffix.length() - 6);
        }
        return "微信用户" + suffix;
    }

    private static String extension(String filename) {
        if (filename == null) return "";
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) return "";
        return filename.substring(idx + 1).toLowerCase();
    }

    private static boolean isAllowedImageExt(String ext) {
        return "png".equals(ext)
                || "jpg".equals(ext)
                || "jpeg".equals(ext)
                || "bmp".equals(ext)
                || "gif".equals(ext)
                || "webp".equals(ext);
    }

    private WxPowerPointLogVO toPowerPointLogVO(AiTokenBillingLogEntity entity) {
        WxPowerPointLogVO vo = new WxPowerPointLogVO();
        vo.setScene(entity.getScene());
        vo.setSceneName(resolveSceneName(entity.getScene()));
        vo.setConsumedPoints(entity.getConsumedPoints() == null ? BigDecimal.ZERO : entity.getConsumedPoints());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }

    private String resolveSceneName(String scene) {
        if (scene == null) {
            return "其他扣费";
        }
        return switch (scene.trim().toUpperCase(Locale.ROOT)) {
            case "CHAT" -> "AI对话";
            case "RAG" -> "RAG对话";
            case "RESUME" -> "简历助手";
            case "POST" -> "AI发帖助手";
            case "INTERNAL" -> "系统内部生成";
            default -> "其他扣费";
        };
    }
}

