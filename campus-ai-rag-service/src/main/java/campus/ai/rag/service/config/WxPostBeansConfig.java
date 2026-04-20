package campus.ai.rag.service.config;

import campus.ai.rag.service.properties.IflyTextAuditProperties;
import campus.ai.rag.service.properties.IflyImageAuditProperties;
import campus.ai.rag.service.properties.WxPostProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({WxPostProperties.class, IflyTextAuditProperties.class, IflyImageAuditProperties.class})
public class WxPostBeansConfig {
}
