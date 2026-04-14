package campus.ai.rag.service.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate配置类，使用 HttpClient5 连接池，支持高并发
 */
@Configuration

public class RestConfig {

    // 注册 RestTemplate Bean：
    // - 业务代码通过 @Autowired RestTemplate 注入使用
    // - 底层使用自定义的 HttpRequestFactory（基于 HttpClient5）
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate(httpRequestFactory());
    }

    // 注册 HttpRequestFactory Bean（RestTemplate 的底层 HTTP 客户端适配器）：
    // - 配置连接池大小、超时策略、空闲连接清理等
    @Bean
    public HttpComponentsClientHttpRequestFactory httpRequestFactory() {
        // 1) 连接池管理器：用于复用 HTTP 连接，提升性能
        PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
        // 最大总连接数：整个连接池最多可同时维护的连接数量
        connManager.setMaxTotal(200); // 最大总连接数
        // 每个路由最大连接数：单个目标主机（host:port）的最大并发连接数
        connManager.setDefaultMaxPerRoute(100); // 每个路由最大连接数

        // 2) 请求配置：统一设置超时策略（避免请求无限挂起）
        RequestConfig requestConfig = RequestConfig.custom()
                // 建立 TCP 连接超时（连接不上服务器时的等待时间）
                .setConnectTimeout(Timeout.ofSeconds(10))       // 建立连接超时
                // 响应超时：从发出请求到收到响应的最大等待时间（这里较长：5 分钟）
                .setResponseTimeout(Timeout.ofSeconds(300))     // 响应超时（增加到5分钟）
                // 从连接池获取连接超时：连接池满/无可用连接时的等待时间
                .setConnectionRequestTimeout(Timeout.ofSeconds(10)) // 从连接池获取连接超时
                .build();

        // 3) 构建 HttpClient 实例：绑定连接池与默认请求配置
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connManager)
                .setDefaultRequestConfig(requestConfig)
                // 清理空闲连接：避免连接长期空闲占用资源（30s 未使用的连接会被回收）
                .evictIdleConnections(Timeout.ofSeconds(30)) // 清理空闲连接
                .build();

        // 4) 包装为 Spring 的 HttpRequestFactory，供 RestTemplate 使用
        return new HttpComponentsClientHttpRequestFactory(httpClient);
    }
}
