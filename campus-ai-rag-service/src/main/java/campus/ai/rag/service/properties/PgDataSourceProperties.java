package campus.ai.rag.service.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "pg.datasource")
public class PgDataSourceProperties {

    private String driverClassName;

    private String url;

    private String username;

    private String password;
}

