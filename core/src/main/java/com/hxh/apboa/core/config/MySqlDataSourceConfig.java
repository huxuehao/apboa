package com.hxh.apboa.core.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceAutoConfigure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * MySQL主数据源配置，确保多数据源场景下MySQL为主数据源
 *
 * @author huxuehao
 */
@Configuration
@AutoConfigureBefore({DataSourceAutoConfiguration.class, DruidDataSourceAutoConfigure.class})
public class MySqlDataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(MySqlDataSourceConfig.class);

    @Bean
    @Primary
    public DataSource dataSource(
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password,
            @Value("${spring.datasource.driver-class-name}") String driverClassName,
            @Value("${spring.datasource.druid.initial-size:5}") int initialSize,
            @Value("${spring.datasource.druid.min-idle:5}") int minIdle,
            @Value("${spring.datasource.druid.max-active:20}") int maxActive,
            @Value("${spring.datasource.druid.max-wait:60000}") long maxWait,
            @Value("${spring.datasource.druid.validation-query:SELECT 1}") String validationQuery) {

        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName(driverClassName);
        dataSource.setInitialSize(initialSize);
        dataSource.setMinIdle(minIdle);
        dataSource.setMaxActive(maxActive);
        dataSource.setMaxWait(maxWait);
        dataSource.setValidationQuery(validationQuery);
        log.info("MySQL主数据源初始化完成, url={}", url);
        return dataSource;
    }
}
