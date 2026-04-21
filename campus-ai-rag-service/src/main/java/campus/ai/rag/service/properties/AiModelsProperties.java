package campus.ai.rag.service.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai")
public class AiModelsProperties {

    /**
     * 默认模型编码（例如：qwen / spark）。
     */
    private String defaultModelCode = "qwen plus";

    public String getDefaultModelCode() {
        return defaultModelCode;
    }

    public void setDefaultModelCode(String defaultModelCode) {
        this.defaultModelCode = defaultModelCode;
    }
}

