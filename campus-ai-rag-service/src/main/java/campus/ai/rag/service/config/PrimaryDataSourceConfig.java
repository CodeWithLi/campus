package campus.ai.rag.service.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * 多数据源时，任意一个 {@link DataSource} Bean 都会让 Spring Boot 不再自动创建 spring.datasource 主库。
 * {@link PgVectorConfig} 注册了 PostgreSQL 后，必须显式声明主库（MySQL）为 {@link Primary}，否则 MyBatis 会误连 PG。
 * <p>
 * 必须用 {@link DataSourceProperties#initializeDataSourceBuilder()} 创建连接池，才能把 {@code spring.datasource.url}
 * 正确映射为 HikariCP 的 {@code jdbcUrl}；仅用 {@code DataSourceBuilder.create().build()} 会导致 “jdbcUrl is required”。
 */
@Configuration
public class PrimaryDataSourceConfig {

    @Bean
    @Primary
    public DataSource dataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(@Qualifier("dataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
