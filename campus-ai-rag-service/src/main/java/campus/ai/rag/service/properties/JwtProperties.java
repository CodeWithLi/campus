/**
 * 该文件的作用：用于读取并封装 JWT 相关配置项（密钥、有效期、Token 名称）。
 * 通常配合 `application.yml`/`application.properties` 中以 `zw.jwt.*` 为前缀的配置使用。
 */
package campus.ai.rag.service.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


@Component
@Data
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String orSecretKey;

    private long orTtl;

    private String orTokenName;

}