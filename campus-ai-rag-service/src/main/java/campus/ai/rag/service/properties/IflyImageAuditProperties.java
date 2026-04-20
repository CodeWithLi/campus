package campus.ai.rag.service.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 讯飞图片合规（image）配置。
 */
@ConfigurationProperties(prefix = "app.ifly.image-audit")
public class IflyImageAuditProperties {

    private boolean enabled = false;

    private String url = "https://audit.iflyaisol.com/audit/v2/image";

    private String accessKeyId = "";

    private String accessKeySecret = "";

    private String appId = "";

    /**
     * 已通过图片合规结果缓存天数（按图片字节 SHA-256 判重）。
     */
    private int passCacheDays = 30;

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

    public int getPassCacheDays() {
        return passCacheDays;
    }

    public void setPassCacheDays(int passCacheDays) {
        this.passCacheDays = passCacheDays;
    }

    public boolean isConfigured() {
        return enabled
                && accessKeyId != null && !accessKeyId.isBlank()
                && accessKeySecret != null && !accessKeySecret.isBlank()
                && appId != null && !appId.isBlank();
    }
}
