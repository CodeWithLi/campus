package campus.ai.rag.service.config;

import campus.ai.rag.service.ai.AiChatRouter;
import campus.ai.rag.service.mapper.model.AiModelConfigMapper;
import campus.ai.rag.service.properties.AiModelsProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AiModelsProperties.class)
public class AiRoutingConfig {

    @Bean
    public AiChatRouter aiChatRouter(AiModelsProperties properties,
                                     AiModelConfigMapper aiModelConfigMapper) {
        AiChatRouter router = new AiChatRouter(properties.getDefaultModelCode(), aiModelConfigMapper);
        router.validate();
        return router;
    }
}

