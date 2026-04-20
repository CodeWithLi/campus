package campus.ai.rag.service.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 发帖 AI 与正文限制（微信端）。
 */
@ConfigurationProperties(prefix = "app.wx.post")
public class WxPostProperties {

    /**
     * 发帖助手使用的模型编码，须对应 ai_model_config 中 invoke_type=SpringAI 的配置（默认 qwen_plus）。
     */
    private String aiModelCode = "qwen_plus";

    /**
     * 帖子正文最大字符数（与前端展示一致，按 Java 字符串长度计）。
     */
    private int maxContentLength = 1000;

    /**
     * 标题最大长度。
     */
    private int maxTitleLength = 80;

    /**
     * AI 生成草稿在 Redis 中的有效期（用于免文本合规校验的可信窗口）。
     */
    private int draftTtlHours = 2;

    /**
     * 单帖最多图片数。
     */
    private int maxImageCount = 5;

    /**
     * 图片存储根目录（本地）。
     */
    private String uploadDir = "upload";

    public String getAiModelCode() {
        return aiModelCode;
    }

    public void setAiModelCode(String aiModelCode) {
        this.aiModelCode = aiModelCode;
    }

    public int getMaxContentLength() {
        return maxContentLength;
    }

    public void setMaxContentLength(int maxContentLength) {
        this.maxContentLength = maxContentLength;
    }

    public int getMaxTitleLength() {
        return maxTitleLength;
    }

    public void setMaxTitleLength(int maxTitleLength) {
        this.maxTitleLength = maxTitleLength;
    }

    public int getDraftTtlHours() {
        return draftTtlHours;
    }

    public void setDraftTtlHours(int draftTtlHours) {
        this.draftTtlHours = draftTtlHours;
    }

    public int getMaxImageCount() {
        return maxImageCount;
    }

    public void setMaxImageCount(int maxImageCount) {
        this.maxImageCount = maxImageCount;
    }

    public String getUploadDir() {
        return uploadDir;
    }

    public void setUploadDir(String uploadDir) {
        this.uploadDir = uploadDir;
    }
}
