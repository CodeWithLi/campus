package campus.ai.rag.service.service.wx.auth.strategy;

import campus.ai.rag.service.common.auth.LoginTokenInfo;
import campus.ai.rag.service.entity.wx.WxUserEntity;
import campus.ai.rag.service.properties.JwtProperties;
import campus.ai.rag.service.service.security.TokenStoreService;
import campus.ai.rag.service.utils.JwtUtils;
import campus.ai.rag.service.vo.admin.AdminLoginVO;
import org.springframework.stereotype.Component;

@Component
public class WxLoginTokenIssuer {

    private static final String CLIENT_TYPE_WX = "WX";

    private final JwtUtils jwtUtils;
    private final JwtProperties jwtProperties;
    private final TokenStoreService tokenStoreService;

    public WxLoginTokenIssuer(JwtUtils jwtUtils, JwtProperties jwtProperties, TokenStoreService tokenStoreService) {
        this.jwtUtils = jwtUtils;
        this.jwtProperties = jwtProperties;
        this.tokenStoreService = tokenStoreService;
    }

    public AdminLoginVO issue(WxUserEntity userEntity) {
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
}

