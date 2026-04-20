package campus.ai.rag.service.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.wx.reminder")
public class WxReminderProperties {

    private Integer advanceMinutes = 30;

    private Subscribe subscribe = new Subscribe();

    @Data
    public static class Subscribe {
        private Boolean enabled = false;
        private String templateId = "";
        private String page = "pages/notification-center/notification-center";
        private String miniprogramState = "formal";
        private String lang = "zh_CN";
        private String titleKey = "thing1";
        private String timeKey = "time2";
        private String typeKey = "thing3";
    }
}
