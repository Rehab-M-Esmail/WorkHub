package com.example.WorkHub.config.multitenancy;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;


@Component
@Order(1)
public class TenantFilter implements Filter {
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException{
        HttpServletRequest request = (HttpServletRequest) req;
        String tenantIdHeader = request.getHeader("X-Tenant-ID");
        if (request.getRequestURI().startsWith("/tenant")|| request.getRequestURI().startsWith("/actuator")|| request.getRequestURI().startsWith("/user")||request.getRequestURI().startsWith("/swagger-ui") || request.getRequestURI().startsWith("/v3/api-docs")) {
            chain.doFilter(req, res);
            return;
        }
        if (tenantIdHeader == null || tenantIdHeader.describeConstable().isEmpty()){
            ((HttpServletResponse) res).sendError(400, "Missing tenant ID");
            return;
        }
        Long tenantId = Long.valueOf(tenantIdHeader);
        TenantContext.setTenantId(Long.valueOf(tenantIdHeader));
        try{
            chain.doFilter(req,res);
        }
        finally {
            TenantContext.clear();
        }
    }
}
