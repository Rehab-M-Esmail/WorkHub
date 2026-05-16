package com.example.WorkHub.config.multitenancy;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;


@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TenantFilter implements Filter {
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException{
        HttpServletRequest request = (HttpServletRequest) req;
        String tenantIdHeader = TenantHeaderResolver.resolve(request);
        if (request.getRequestURI().startsWith("/tenant")|| request.getRequestURI().startsWith("/actuator")|| request.getRequestURI().startsWith("/user")||request.getRequestURI().startsWith("/swagger-ui") || request.getRequestURI().startsWith("/v3/api-docs")) {
            chain.doFilter(req, res);
            return;
        }
        if (tenantIdHeader == null || tenantIdHeader.describeConstable().isEmpty()) {
            ((HttpServletResponse) res).sendError(400, "Missing tenant ID");
            return;
        }
        TenantContext.setTenantId(Long.valueOf(tenantIdHeader));
        try{
            chain.doFilter(req,res);
        }
        finally {
            TenantContext.clear();
        }
    }
}
