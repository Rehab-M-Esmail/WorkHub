package com.example.WorkHub.config.multitenancy;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DynamicTenantDataSource extends AbstractRoutingDataSource {
    private final Map tenantDataSources = new ConcurrentHashMap();

    public DynamicTenantDataSource(String masterUrl, String masterUser, String masterPass){
        HikariDataSource masterDs = buildDataSource(masterUrl, masterUser, masterPass);
        tenantDataSources.put("master", masterDs);
        setTargetDataSources(tenantDataSources);
        setDefaultTargetDataSource(masterDs);
        afterPropertiesSet();
    }

    @Override
    protected Object determineCurrentLookupKey(){
        return TenantContext.getTenantId();
    }

    public void registerTenant(String tenantName, DataSource dataSource){
        tenantDataSources.put(tenantName, dataSource);
        setTargetDataSources(tenantDataSources);
        afterPropertiesSet();
    }

    public boolean tenantExists(String tenantName){
        return tenantDataSources.containsKey(tenantName);
    }

    private HikariDataSource buildDataSource(String url, String user, String pass){
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(url);
        ds.setUsername(user);
        ds.setPassword(pass);
        ds.setDriverClassName("org.postgresql.Driver");
        return ds;
    }
}
