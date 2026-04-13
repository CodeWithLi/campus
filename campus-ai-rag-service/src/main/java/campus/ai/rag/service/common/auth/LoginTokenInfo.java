package campus.ai.rag.service.common.auth;

import java.io.Serializable;

/**
 * 登录令牌信息：存入 Redis，用于后续请求鉴权。
 */
public class LoginTokenInfo implements Serializable {

    private String token;
    private String userId;
    private String username;
    private String roleCode;
    private String clientType;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public String getClientType() {
        return clientType;
    }

    public void setClientType(String clientType) {
        this.clientType = clientType;
    }
}

