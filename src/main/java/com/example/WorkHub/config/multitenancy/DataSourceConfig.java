package com.example.WorkHub.config.multitenancy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class DataSourceConfig {

    @Value("${master.datasource.url}")
    private String masterUrl;

    @Value("${master.datasource.username}")
    private String masterUser;

    @Value("${master.datasource.password}")
    private String masterPass;

    @Bean
    @Primary
    public DynamicTenantDataSource dataSource() {
        return new DynamicTenantDataSource(masterUrl, masterUser, masterPass);
    }
}
