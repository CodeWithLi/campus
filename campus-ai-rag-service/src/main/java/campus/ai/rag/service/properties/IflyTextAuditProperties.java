package campus.ai.rag.service.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 讯飞文本合规（syncText）可选配置；未启用时不调用外网。
 */
@ConfigurationProperties(prefix = "app.ifly.text-audit")
public class IflyTextAuditProperties {

    private boolean enabled = false;

    private String url = "https://audit.iflyaisol.com/audit/v2/syncText";

    private String accessKeyId = "";

    private String accessKeySecret = "";

    private String appId = "";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getAccessKeySecret() {
        return accessKeySecret;
    }

    public void setAccessKeySecret(String accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public boolean isConfigured() {
        return enabled
                && accessKeyId != null && !accessKeyId.isBlank()
                && accessKeySecret != null && !accessKeySecret.isBlank()
                && appId != null && !appId.isBlank();
    }
}
