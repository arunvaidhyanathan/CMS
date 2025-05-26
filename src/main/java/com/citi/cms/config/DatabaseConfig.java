package com.citi.cms.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EnableJpaRepositories(basePackages = "com.citi.cms.repository")
@EnableJpaAuditing
@EnableTransactionManagement
public class DatabaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);

    @Value("${spring.datasource.url}")
    private String databaseUrl;

    @Value("${spring.datasource.username}")
    private String databaseUsername;

    @Value("${spring.datasource.password}")
    private String databasePassword;

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    @Value("${spring.datasource.hikari.maximum-pool-size:20}")
    private int maximumPoolSize;

    @Value("${spring.datasource.hikari.minimum-idle:5}")
    private int minimumIdle;

    @Value("${spring.datasource.hikari.connection-timeout:20000}")
    private long connectionTimeout;

    @Value("${spring.datasource.hikari.idle-timeout:300000}")
    private long idleTimeout;

    @Value("${spring.datasource.hikari.max-lifetime:1800000}")
    private long maxLifetime;

    @Bean
    @Primary
    @Profile("!test")
    public DataSource primaryDataSource() {
        logger.info("Configuring primary PostgreSQL DataSource");
        
        HikariConfig config = new HikariConfig();
        
        // Basic connection properties
        config.setJdbcUrl(databaseUrl);
        config.setUsername(databaseUsername);
        config.setPassword(databasePassword);
        config.setDriverClassName(driverClassName);
        
        // Pool configuration
        config.setMaximumPoolSize(maximumPoolSize);
        config.setMinimumIdle(minimumIdle);
        config.setConnectionTimeout(connectionTimeout);
        config.setIdleTimeout(idleTimeout);
        config.setMaxLifetime(maxLifetime);
        
        // Connection test query
        config.setConnectionTestQuery("SELECT 1");
        
        // Pool name
        config.setPoolName("CMS-PostgreSQL-Pool");
        
        // Additional HikariCP optimizations
        config.setLeakDetectionThreshold(60000); // 60 seconds
        config.setValidationTimeout(3000); // 3 seconds
        
        // PostgreSQL specific optimizations
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");
        
        // SSL and security settings for PostgreSQL
        config.addDataSourceProperty("sslmode", "require");
        config.addDataSourceProperty("ApplicationName", "CMS-Backend");
        
        logger.info("PostgreSQL DataSource configured with URL: {}", maskUrl(databaseUrl));
        logger.info("Connection pool configured - Max: {}, Min: {}", maximumPoolSize, minimumIdle);
        
        return new HikariDataSource(config);
    }

    @Bean
    @Profile("test")
    public DataSource testDataSource() {
        logger.info("Configuring test H2 DataSource");
        
        HikariConfig config = new HikariConfig();
        
        // H2 in-memory database for testing
        config.setJdbcUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        config.setUsername("sa");
        config.setPassword("");
        config.setDriverClassName("org.h2.Driver");
        
        // Smaller pool for testing
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(5000);
        
        config.setPoolName("CMS-Test-H2-Pool");
        
        logger.info("Test H2 DataSource configured");
        
        return new HikariDataSource(config);
    }

    @Bean
    @Profile("dev")
    public DataSource devDataSource() {
        logger.info("Configuring development DataSource");
        
        HikariConfig config = new HikariConfig();
        
        // Development database configuration
        config.setJdbcUrl(databaseUrl);
        config.setUsername(databaseUsername);
        config.setPassword(databasePassword);
        config.setDriverClassName(driverClassName);
        
        // Smaller pool for development
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(connectionTimeout);
        config.setIdleTimeout(idleTimeout);
        config.setMaxLifetime(maxLifetime);
        
        config.setPoolName("CMS-Dev-Pool");
        
        // Development specific settings
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "100");
        
        logger.info("Development DataSource configured with URL: {}", maskUrl(databaseUrl));
        
        return new HikariDataSource(config);
    }

    /**
     * Masks sensitive information in database URL for logging
     */
    private String maskUrl(String url) {
        if (url == null) return "null";
        
        // Mask password in URL if present
        return url.replaceAll("password=[^&;]*", "password=***");
    }
}