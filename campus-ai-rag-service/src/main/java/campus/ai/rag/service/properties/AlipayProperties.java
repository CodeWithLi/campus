package campus.ai.rag.service.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "alipay")
public class AlipayProperties {

    private String appId;

    private String gatewayUrl;

    private String notifyUrl;

    private String returnUrl;

    private String signType = "RSA2";

    private String charset = "UTF-8";

    private String format = "json";

    /**
     * classpath 路径，如：alipay/app_private_key_pkcs8.txt
     */
    private String privateKeyPath;

    /**
     * classpath 路径，如：alipay/alipay_public_key.txt
     */
    private String alipayPublicKeyPath;
}

