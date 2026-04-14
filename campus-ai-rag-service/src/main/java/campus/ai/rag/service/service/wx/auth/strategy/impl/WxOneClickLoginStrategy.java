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
import campus.ai.rag.service.utils.ApiService;
import campus.ai.rag.service.vo.admin.AdminLoginVO;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

@Component
public class WxOneClickLoginStrategy implements WxLoginStrategy {

    private static final String LOGIN_TYPE = "WX_ONE_CLICK";
    private static final String AUTH_TYPE_WX_OPENID = "WX_OPENID";

    private final ApiService apiService;
    private final WxUserAuthMapper wxUserAuthMapper;
    private final WxUserMapper wxUserMapper;
    private final WxLoginTokenIssuer wxLoginTokenIssuer;

    public WxOneClickLoginStrategy(ApiService apiService,
                                   WxUserAuthMapper wxUserAuthMapper,
                                   WxUserMapper wxUserMapper,
                                   WxLoginTokenIssuer wxLoginTokenIssuer) {
        this.apiService = apiService;
        this.wxUserAuthMapper = wxUserAuthMapper;
        this.wxUserMapper = wxUserMapper;
        this.wxLoginTokenIssuer = wxLoginTokenIssuer;
    }

    @Override
    public String supportType() {
        return LOGIN_TYPE;
    }

    @Override
    public AdminLoginVO login(WxLoginDTO dto) {
        if (dto == null || isBlank(dto.getWxCode())) {
            throw new BusinessException(StatusCodeEnum.BAD_REQUEST, "微信登录code不能为空");
        }

        String openid = resolveOpenid(dto.getWxCode().trim());
        WxUserAuthEntity authEntity = wxUserAuthMapper.findByAuthTypeAndAuthKey(AUTH_TYPE_WX_OPENID, openid);
        if (Objects.isNull(authEntity) || !Objects.equals(authEntity.getEnabled(), 1)) {
            throw new BusinessException(StatusCodeEnum.NOT_FOUND, "该微信未绑定账号，请先注册");
        }
        if (!Objects.equals(authEntity.getAllowLogin(), 1)) {
            throw new BusinessException(StatusCodeEnum.FORBIDDEN, "当前账号暂不允许使用微信一键登录");
        }

        WxUserEntity userEntity = wxUserMapper.selectById(authEntity.getUserId());
        if (Objects.isNull(userEntity) || !Objects.equals(userEntity.getStatus(), 1)) {
            throw new BusinessException(StatusCodeEnum.UNAUTHORIZED, "用户不存在或已禁用");
        }
        return wxLoginTokenIssuer.issue(userEntity);
    }

    private String resolveOpenid(String wxCode) {
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

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

